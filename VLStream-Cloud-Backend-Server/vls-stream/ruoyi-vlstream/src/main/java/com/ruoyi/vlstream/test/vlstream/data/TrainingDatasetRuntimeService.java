package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import javax.annotation.Resource;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Materializes verified immutable packages on compute hosts; never extracts an archive locally. */
@Service
@RequiredArgsConstructor
public class TrainingDatasetRuntimeService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_METADATA_BYTES = 1024 * 1024;
    private static final long MAX_ENTRY_BYTES = 128L * 1024 * 1024;
    private static final String MARKER = "vls-artifact.json";
    private static final String[] DEFAULT_SCRIPTS = {"four_task_runtime.py", "run_training.py"};
    private final TrainingDatasetArtifactService artifacts;
    private final VlsSshProperties ssh;
    private final DatasetRemoteCleanup remoteCleanup;
    private final JdbcTemplate jdbc;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Value("${vlstream.training-dataset.max-expanded-bytes:34359738368}")
    private long maxExpandedBytes = 32L * 1024 * 1024 * 1024;

    public String prepareDefault(Long datasetId, String ref) throws IOException {
        TrainingDatasetArtifact artifact = artifacts.require(datasetId, ref);
        String root = DatasetRemoteCleanup.root(datasetId) + "/artifact_" + artifact.getId() + "_" + UUID.randomUUID();
        validateDefaultPath(datasetId, artifact, root + "/dataset.yaml");
        try (ComputeSsh.Connection connection = openDefault()) {
            recordRemoteUsage(datasetId, artifact);
            return materialize(artifact, root, connection, DEFAULT_SCRIPTS);
        }
    }

    public String restoreDefault(Long datasetId, String ref, String runtimeDatasetPath) throws IOException {
        TrainingDatasetArtifact artifact = artifacts.require(datasetId, ref);
        String root = validateDefaultPath(datasetId, artifact, runtimeDatasetPath);
        try (ComputeSsh.Connection connection = openDefault()) {
            recordRemoteUsage(datasetId, artifact);
            checkRemoteRoot(connection, root);
            if (matchingMarker(connection, root, artifact) && ready(connection, root, DEFAULT_SCRIPTS)) return runtimeDatasetPath;
            return materialize(artifact, root, connection, DEFAULT_SCRIPTS);
        }
    }

    public String upload(TrainingDatasetArtifact artifact, String root, ComputeSsh.Connection connection) throws IOException {
        return materialize(artifact, root, connection);
    }

    protected ComputeSsh.Connection openDefault() throws IOException {
        Session session = null;
        try {
            session = new JSch().getSession(ssh.getUsername(), ssh.getHost(), ssh.getPort());
            session.setPassword(ssh.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(30000);
            session.connect(20000);
            return new ComputeSsh.Connection(session);
        } catch (Exception ex) {
            if (session != null) session.disconnect();
            throw new IOException("默认训练服务器 SSH 连接失败", ex);
        }
    }

    private String materialize(TrainingDatasetArtifact artifact, String root, ComputeSsh.Connection connection,
                               String... scripts) throws IOException {
        absoluteRoot(root);
        if (artifact == null || artifact.getSha256() == null || !artifact.getSha256().matches("[0-9a-fA-F]{64}"))
            throw new IOException("训练数据集制品校验信息缺失");
        Path local = artifacts.download(artifact);
        try (ZipFile zip = new ZipFile(local.toFile(), StandardCharsets.UTF_8)) {
            Inventory inventory = inspect(zip);
            String runtimeYaml = runtimeYaml(artifact.getDatasetYaml(), inventory.metadata.get("dataset.yaml"), root);
            String calibration = calibration(inventory, root);
            // Inspect the complete archive before the first remote write or directory creation.
            checkRemoteRoot(connection, root);
            matchingMarker(connection, root, artifact);
            checkRemoteTargets(connection, root, inventory.entries);
            connection.mkdir(root);
            connection.execute("rm -f -- " + quote(root + "/" + MARKER), 30);
            Set<String> directories = new HashSet<>();
            directories.add(root);
            for (ZipEntry entry : inventory.entries) {
                String name = entry.getName();
                String target = root + "/" + (entry.isDirectory() ? name.substring(0, name.length() - 1) : name);
                if (entry.isDirectory()) {
                    makeDirectory(connection, target, directories);
                } else {
                    makeDirectory(connection, target.substring(0, target.lastIndexOf('/')), directories);
                    if ("dataset.yaml".equals(name)) connection.putText(target, runtimeYaml);
                    else if ("coco_subset_20.txt".equals(name)) connection.putText(target, calibration);
                    else try (InputStream input = zip.getInputStream(entry)) { connection.put(target, input); }
                }
            }
            copyScripts(connection, root, scripts);
            Map<String, String> marker = new LinkedHashMap<>();
            marker.put("reference", artifact.getReference());
            marker.put("sha256", artifact.getSha256());
            connection.putText(root + "/" + MARKER, JSON.writeValueAsString(marker));
            return root + "/dataset.yaml";
        } finally { Files.deleteIfExists(local); }
    }

    public static void copyScripts(ComputeSsh.Connection connection, String root, String... names) throws IOException {
        absoluteRoot(root);
        for (String name : names) {
            if (name == null || !name.matches("[a-z][a-z0-9_]*\\.py")) throw new IOException("训练运行脚本名称无效");
            try (InputStream input = TrainingDatasetRuntimeService.class.getClassLoader().getResourceAsStream("training/" + name)) {
                if (input == null) throw new IOException("缺少平台训练运行脚本：" + name);
                connection.put(root + "/" + name, input);
            }
        }
    }

    private Inventory inspect(ZipFile zip) throws IOException {
        Inventory inventory = new Inventory();
        Set<String> explicit = new HashSet<>(), directories = new HashSet<>(), files = new HashSet<>();
        long total = 0;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (inventory.entries.size() >= 100000) throw new IOException("训练数据包文件数超过 100000");
            String name = relative(entry.getName(), entry.isDirectory());
            if (!explicit.add(name)) throw new IOException("训练数据包包含重复路径");
            if (entry.isDirectory()) {
                if (files.contains(name)) throw new IOException("训练数据包文件与目录冲突");
                directories.add(name);
            } else {
                if (directories.contains(name)) throw new IOException("训练数据包文件与目录冲突");
                if (!name.matches("(?i).+\\.(jpg|jpeg|png|bmp|gif|webp|txt|yaml|yml|json|cache|npy)") || MARKER.equals(name))
                    throw new IOException("训练数据包包含执行脚本或非数据文件");
                files.add(name);
            }
            for (int slash = name.lastIndexOf('/'); slash >= 0; slash = name.lastIndexOf('/', slash - 1)) {
                String parent = name.substring(0, slash);
                if (files.contains(parent)) throw new IOException("训练数据包文件与目录冲突");
                directories.add(parent);
            }
            if (entry.getSize() < 0 || entry.getSize() > MAX_ENTRY_BYTES || entry.getCrc() < 0)
                throw new IOException("训练数据包文件大小或校验信息无效");
            if (entry.isDirectory() && entry.getSize() != 0) throw new IOException("训练数据包目录包含内容");
            boolean metadata = "dataset.yaml".equals(name) || "coco_subset_20.txt".equals(name);
            if (metadata && entry.getSize() > MAX_METADATA_BYTES) throw new IOException("训练元数据超过 1 MiB");
            ByteArrayOutputStream captured = metadata ? new ByteArrayOutputStream() : null;
            CRC32 crc = new CRC32();
            long size = 0;
            try (InputStream input = zip.getInputStream(entry)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) {
                    size += count; total += count;
                    if (size > MAX_ENTRY_BYTES || total > maxExpandedBytes || (metadata && size > MAX_METADATA_BYTES))
                        throw new IOException("训练数据包展开大小超过限制");
                    crc.update(buffer, 0, count);
                    if (captured != null) captured.write(buffer, 0, count);
                }
            }
            if (size != entry.getSize() || crc.getValue() != entry.getCrc()) throw new IOException("训练数据包 CRC 或文件大小校验失败");
            if (captured != null) inventory.metadata.put(name,
                StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(captured.toByteArray())).toString());
            inventory.entries.add(entry);
        }
        if (!inventory.metadata.containsKey("dataset.yaml") || !inventory.metadata.containsKey("coco_subset_20.txt"))
            throw new IOException("训练数据包缺少配置或校准列表");
        inventory.files.addAll(files);
        return inventory;
    }

    private static String runtimeYaml(String frozen, String archived, String root) throws IOException {
        if (frozen == null || !frozen.equals(archived)) throw new IOException("训练数据包配置与冻结版本不一致");
        if (frozen.getBytes(StandardCharsets.UTF_8).length > MAX_METADATA_BYTES) throw new IOException("训练元数据超过 1 MiB");
        try {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false); options.setMaxAliasesForCollections(0); options.setCodePointLimit(MAX_METADATA_BYTES);
            DumperOptions dumper = new DumperOptions(); dumper.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(new SafeConstructor(options), new Representer(dumper), dumper, options);
            Object parsed = yaml.load(frozen);
            if (!(parsed instanceof Map)) throw new IOException("训练数据集 YAML 无效");
            Map<Object, Object> runtime = new LinkedHashMap<>((Map<?, ?>) parsed);
            for (String split : Arrays.asList("train", "val")) {
                Object path = runtime.get(split);
                if (!(path instanceof String)) throw new IOException("训练数据集划分路径无效");
                relative((String) path, false);
            }
            runtime.put("path", root);
            return yaml.dump(runtime);
        } catch (RuntimeException ex) { throw new IOException("训练数据集 YAML 无效", ex); }
    }

    private static String calibration(Inventory inventory, String root) throws IOException {
        StringBuilder result = new StringBuilder();
        for (String line : inventory.metadata.get("coco_subset_20.txt").split("\\r?\\n")) {
            if (line.isEmpty()) continue;
            String path = relative(line, false);
            if (!inventory.files.contains(path)) throw new IOException("校准列表引用的数据文件不存在");
            result.append(root).append('/').append(path).append('\n');
        }
        if (result.length() == 0) throw new IOException("训练数据集校准列表为空");
        return result.toString();
    }

    private void recordRemoteUsage(Long datasetId, TrainingDatasetArtifact artifact) throws IOException {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty() || !tenant.equals(artifact.getTenantId()) || !datasetId.equals(artifact.getDatasetId()))
            throw new IOException("训练数据集远端工作目录归属不匹配");
        String identity = remoteCleanup.identity();
        if (transactionManager == null) throw new IOException("训练数据集远端目录登记事务不可用");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transaction.execute(status -> {
                jdbc.update("INSERT IGNORE INTO vls_training_dataset_remote_usage(tenant_id,dataset_id,remote_identity) VALUES(?,?,?)", tenant, datasetId, identity);
                jdbc.update("UPDATE vls_training_dataset_remote_usage SET remote_identity=? WHERE tenant_id=? AND dataset_id=? AND (remote_identity IS NULL OR remote_identity='')", identity, tenant, datasetId);
                List<String> recorded = jdbc.queryForList("SELECT remote_identity FROM vls_training_dataset_remote_usage WHERE tenant_id=? AND dataset_id=? FOR UPDATE", String.class, tenant, datasetId);
                if (recorded.size() != 1 || !identity.equals(recorded.get(0)))
                    throw new UncheckedIOException(new IOException("训练服务器配置已变更，已停止物化数据集"));
                return null;
            });
        } catch (UncheckedIOException ex) { throw ex.getCause(); }
    }

    private static String validateDefaultPath(Long datasetId, TrainingDatasetArtifact artifact, String datasetPath) throws IOException {
        String prefix = DatasetRemoteCleanup.root(datasetId) + "/artifact_" + artifact.getId() + "_";
        if (datasetPath == null || !datasetPath.matches(Pattern.quote(prefix) + "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}/dataset\\.yaml"))
            throw new IOException("训练数据集运行路径不属于当前数据集制品");
        String root = datasetPath.substring(0, datasetPath.length() - "/dataset.yaml".length());
        absoluteRoot(root);
        return root;
    }

    private static boolean matchingMarker(ComputeSsh.Connection connection, String root, TrainingDatasetArtifact artifact) throws IOException {
        String marker = quote(root + "/" + MARKER);
        String value = connection.execute("if [ -e " + marker + " ]; then test -f " + marker + " && cat -- " + marker + "; else printf MISSING; fi", 30).trim();
        if ("MISSING".equals(value)) return false;
        JsonNode saved = JSON.readTree(value);
        if (saved == null || !artifact.getReference().equals(saved.path("reference").asText())
            || !artifact.getSha256().equals(saved.path("sha256").asText())) throw new IOException("训练数据集运行目录标记与制品不匹配");
        return true;
    }

    private static boolean ready(ComputeSsh.Connection connection, String root, String... scripts) throws IOException {
        String dataset = quote(root + "/dataset.yaml");
        StringBuilder command = new StringBuilder("test -r ").append(dataset).append(" && test -f ").append(dataset)
            .append(" && test -s ").append(dataset);
        for (String script : scripts) {
            String path = quote(root + "/" + script);
            command.append(" && test -r ").append(path).append(" && test -f ").append(path);
        }
        command.append(" && printf READY || printf INCOMPLETE");
        return "READY".equals(connection.execute(command.toString(), 30).trim());
    }

    private static void checkRemoteRoot(ComputeSsh.Connection connection, String root) throws IOException {
        absoluteRoot(root);
        StringBuilder command = new StringBuilder("set -e; ");
        String current = "";
        for (String part : root.substring(1).split("/")) {
            current += "/" + part;
            command.append("test ! -L ").append(quote(current)).append("; if [ -e ").append(quote(current))
                .append(" ]; then test -d ").append(quote(current)).append("; fi; ");
        }
        command.append("if [ -d ").append(quote(root)).append(" ]; then test -z \"$(find ").append(quote(root))
            .append(" -mindepth 1 ! -type f ! -type d -print -quit)\"; fi");
        connection.execute(command.toString(), 30);
    }

    private static void checkRemoteTargets(ComputeSsh.Connection connection, String root, List<ZipEntry> entries) throws IOException {
        StringBuilder command = new StringBuilder("set -e; ");
        int count = 0;
        for (ZipEntry entry : entries) {
            String name = entry.isDirectory() ? entry.getName().substring(0, entry.getName().length() - 1) : entry.getName();
            String target = quote(root + "/" + name);
            command.append("if [ -e ").append(target).append(" ]; then test ")
                .append(entry.isDirectory() ? "-d " : "-f ").append(target).append("; fi; ");
            if (++count == 64) {
                connection.execute(command.toString(), 30);
                command = new StringBuilder("set -e; "); count = 0;
            }
        }
        if (count != 0) connection.execute(command.toString(), 30);
    }

    private static void makeDirectory(ComputeSsh.Connection connection, String path, Set<String> created) throws IOException {
        if (created.add(path)) connection.mkdir(path);
    }

    private static void absoluteRoot(String root) throws IOException {
        if (root == null || !root.startsWith("/") || root.length() == 1) throw new IOException("训练工作目录必须是绝对路径");
        relative(root.substring(1), false);
    }

    private static String relative(String path, boolean directory) throws IOException {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0 || path.indexOf(':') >= 0)
            throw new IOException("训练数据包包含不安全路径");
        String result = directory && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        for (String part : result.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) throw new IOException("训练数据包包含不安全路径");
            for (int i = 0; i < part.length(); i++) if (Character.isISOControl(part.charAt(i))) throw new IOException("训练数据包路径包含控制字符");
        }
        return result;
    }

    private static String quote(String value) { return ComputeSsh.quote(value); }

    private static final class Inventory {
        private final List<ZipEntry> entries = new ArrayList<>();
        private final Map<String, String> metadata = new HashMap<>();
        private final Set<String> files = new HashSet<>();
    }
}
