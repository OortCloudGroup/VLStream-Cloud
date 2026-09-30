package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds portable data only; execution scripts and compute-machine paths never enter the archive. */
@Service
@RequiredArgsConstructor
public class TrainingDatasetArtifactService {
    private static final String PREFIX = "vls-dataset://";
    private static final long IMAGE_LIMIT = 25L * 1024 * 1024;
    private static final String COLUMNS = "id,tenant_id,dataset_id,version_id,annotation_type,storage_state,snapshot_sha256,dataset_yaml,storage_config,storage_bucket,object_key,file_size,sha256";
    private final JdbcTemplate jdbc;
    private final DataMediaStorage media;
    private final SampleMediaInspector inspector;
    private final ObjectMapper json;
    private final PlatformTransactionManager transactions;

    @Value("${vlstream.training-dataset.temp-dir:${java.io.tmpdir}/vls-training-datasets}")
    private String tempDir;

    public static String reference(String artifactId) {
        if (artifactId == null || !artifactId.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))
            throw new ServiceException("训练数据集引用无效");
        return PREFIX + UUID.fromString(artifactId).toString();
    }

    public static boolean isReference(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public TrainingDatasetArtifact require(Long datasetId, String ref) {
        if (datasetId == null) throw new ServiceException("缺少训练数据集编号");
        List<TrainingDatasetArtifact> rows = jdbc.query("SELECT " + COLUMNS + " FROM vls_training_dataset_artifact WHERE tenant_id=? AND dataset_id=? AND id=? AND storage_state='READY'",
            mapper(), tenant(), datasetId, referenceId(ref));
        if (rows.isEmpty()) throw new ServiceException("训练数据集制品不存在、未就绪或不属于当前数据集");
        return rows.get(0);
    }

    public TrainingDatasetArtifact requireReference(String ref) {
        List<TrainingDatasetArtifact> rows = jdbc.query("SELECT " + COLUMNS + " FROM vls_training_dataset_artifact WHERE tenant_id=? AND id=? AND storage_state='READY'",
            mapper(), tenant(), referenceId(ref));
        if (rows.isEmpty()) throw new ServiceException("训练数据集制品不存在、未就绪或不属于当前租户");
        return rows.get(0);
    }

    /** Includes failed uploads so cleanup can remove every tracked object. */
    public List<TrainingDatasetArtifact> listForDataset(Long datasetId) {
        if (datasetId == null) throw new ServiceException("缺少训练数据集编号");
        return jdbc.query("SELECT " + COLUMNS + " FROM vls_training_dataset_artifact WHERE tenant_id=? AND dataset_id=? AND object_key IS NOT NULL",
            mapper(), tenant(), datasetId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TrainingDatasetArtifact ensure(Long datasetId, DatasetVersion version) throws IOException {
        String tenant = tenant();
        if (datasetId == null || version == null || version.getId() == null || !datasetId.equals(version.getAnnotationId()) || !tenant.equals(version.getTenantId()))
            throw new ServiceException("训练版本不属于当前租户或数据集");
        TransactionTemplate claim = new TransactionTemplate(transactions);
        claim.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        BuildWork work;
        try {
            work = claim.execute(status -> {
                try { return claim(datasetId, version.getId(), tenant); }
                catch (IOException ex) { throw new UncheckedIOException(ex); }
            });
        } catch (UncheckedIOException ex) { throw ex.getCause(); }
        if (work == null) throw new IOException("无法登记训练数据集制品");
        TrainingDatasetArtifact existing = work.artifact;
        if ("READY".equals(existing.getState())) return existing;
        String id = existing.getId(), owner = work.owner;
        Path zip = null;
        try {
            zip = temporary("dataset-build-", ".zip");
            build(zip, datasetId, version.getId(), work.saved.getVersionNumber(), tenant, work.snapshot, work.samples, work.layout, work.yaml, id, owner);
            long size = Files.size(zip);
            String sha = DatasetFileIO.sha256(zip);
            // Preserve the original config and bucket across retries, even if the platform default changes.
            OssClient storage = client(existing.getStorageConfig());
            if (hasText(existing.getStorageBucket()) && !existing.getStorageBucket().equals(storage.getBucketName())) throw new IOException("训练数据集对象存储桶配置已改变");
            if (!hasText(storage.getConfigKey()) || !hasText(storage.getBucketName())) throw new IOException("训练数据集对象存储配置无效");
            String object = "datasets/" + hash(tenant.getBytes(StandardCharsets.UTF_8)) + "/" + datasetId + "/" + version.getId() + "/" + id + ".zip";
            renew(tenant, id, owner);
            // Register the key before any remote write, including failed and interrupted uploads.
            if (jdbc.update("UPDATE vls_training_dataset_artifact SET storage_config=?,storage_bucket=?,object_key=?,file_size=?,sha256=? WHERE tenant_id=? AND id=? AND lease_owner=? AND storage_state='BUILDING' AND lease_until>=NOW()",
                storage.getConfigKey(), storage.getBucketName(), object, size, sha, tenant, id, owner) != 1) throw new IOException("训练数据集归档租约已失效");
            storage.uploadFile(zip.toFile(), object, "application/zip");
            renew(tenant, id, owner);
            if (storage.getObjectMetadata(object).getContentLength() != size) throw new IOException("MinIO 训练数据包大小不一致");
            try (InputStream input = storage.getObjectContent(object)) { verify(input, null, size, sha); }
            if (jdbc.update("UPDATE vls_training_dataset_artifact SET storage_state='READY',lease_owner=NULL,lease_until=NULL,retry_after=NULL,error_message=NULL WHERE tenant_id=? AND id=? AND lease_owner=? AND storage_state='BUILDING' AND lease_until>=NOW()",
                tenant, id, owner) != 1) throw new IOException("训练数据集归档租约已失效");
            return require(datasetId, reference(id));
        } catch (Exception ex) {
            jdbc.update("UPDATE vls_training_dataset_artifact SET storage_state='FAILED',lease_owner=NULL,lease_until=NULL,retry_after=DATE_ADD(NOW(),INTERVAL 1 MINUTE),error_message=? WHERE tenant_id=? AND id=? AND lease_owner=? AND storage_state='BUILDING'",
                "训练数据包生成或回读校验失败：" + ex.getClass().getSimpleName(), tenant, id, owner);
            if (ex instanceof IOException) throw (IOException) ex;
            throw new IOException("训练数据包归档失败", ex);
        } finally { if (zip != null) Files.deleteIfExists(zip); }
    }

    /** The project row is shared with cleanup; commit the lease before writing any remote object. */
    private BuildWork claim(Long datasetId, Long versionId, String tenant) throws IOException {
        if (jdbc.queryForList("SELECT id FROM vls_algorithm_annotation WHERE tenant_id=? AND id=? AND is_deleted=0 FOR UPDATE", Long.class, tenant, datasetId).isEmpty())
            throw new ServiceException("数据集不存在或无权访问");
        Long cleaning = jdbc.queryForObject("SELECT COUNT(*) FROM vls_dataset_cleanup WHERE tenant_id=? AND annotation_id=?", Long.class, tenant, datasetId);
        if (cleaning == null || cleaning > 0) throw new ServiceException("数据集正在清理，不能生成训练数据包");
        List<DatasetVersion> versions = jdbc.query("SELECT snapshot_json,version_number FROM vls_dataset_version WHERE tenant_id=? AND annotation_id=? AND id=? AND is_deleted=0",
            (rs, row) -> { DatasetVersion saved = new DatasetVersion(); saved.setSnapshotJson(rs.getString(1)); saved.setVersionNumber(rs.getInt(2)); return saved; }, tenant, datasetId, versionId);
        if (versions.isEmpty()) throw new ServiceException("训练数据集版本不存在");
        DatasetVersion saved = versions.get(0);
        String snapshotHash = hash(saved.getSnapshotJson().getBytes(StandardCharsets.UTF_8));
        TrainingDatasetArtifact existing = find(tenant, datasetId, versionId);
        if (existing != null && !snapshotHash.equals(existing.getSnapshotSha256())) throw new IOException("训练版本快照已改变，不能覆盖原制品");
        if (existing != null && "READY".equals(existing.getState())) return new BuildWork(existing);
        DatasetSnapshot snapshot = json.readValue(saved.getSnapshotJson(), DatasetSnapshot.class);
        List<AnnotationImage> samples = members(snapshot);
        TrainingDatasetLayout layout = new TrainingDatasetLayout(snapshot.getAnnotationType(), snapshot.getLabels());
        String yaml = layout.yaml(".").replaceFirst("^path: [^\\n]*\\n", "");
        String id = existing == null ? UUID.randomUUID().toString() : existing.getId();
        jdbc.update("INSERT IGNORE INTO vls_training_dataset_artifact(id,tenant_id,dataset_id,version_id,annotation_type,snapshot_sha256,dataset_yaml) VALUES(?,?,?,?,?,?,?)",
            id, tenant, datasetId, versionId, snapshot.getAnnotationType(), snapshotHash, yaml);
        // A concurrent insert may own a different UUID for this immutable version.
        existing = find(tenant, datasetId, versionId);
        if (existing == null || !snapshotHash.equals(existing.getSnapshotSha256())) throw new IOException("无法登记冻结训练数据集版本");
        if ("READY".equals(existing.getState())) return new BuildWork(existing);
        id = existing.getId();
        String owner = UUID.randomUUID().toString();
        if (jdbc.update("UPDATE vls_training_dataset_artifact SET storage_state='BUILDING',lease_owner=?,lease_until=DATE_ADD(NOW(),INTERVAL 6 HOUR),retry_after=NULL,attempts=attempts+1 WHERE tenant_id=? AND id=? AND storage_state<>'READY' AND (lease_until IS NULL OR lease_until<NOW()) AND (retry_after IS NULL OR retry_after<NOW())",
            owner, tenant, id) != 1) throw new IOException("训练数据集正在归档或等待重试，请稍后再试");
        BuildWork work = new BuildWork(existing); work.saved = saved; work.snapshot = snapshot; work.samples = samples; work.layout = layout; work.yaml = yaml; work.owner = owner;
        return work;
    }

    /** Resolves metadata again rather than trusting a caller-supplied object key. Caller deletes the returned file. */
    public Path download(TrainingDatasetArtifact artifact) throws IOException {
        if (artifact == null) throw new ServiceException("缺少训练数据集制品");
        TrainingDatasetArtifact saved = require(artifact.getDatasetId(), artifact.getReference());
        if (!hasText(saved.getStorageConfig()) || !hasText(saved.getStorageBucket()) || !hasText(saved.getObjectKey()) || saved.getFileSize() == null || saved.getFileSize() <= 0 || !hasText(saved.getSha256()))
            throw new IOException("训练数据集存储元数据不完整");
        Path local = null;
        try {
            OssClient storage = client(saved.getStorageConfig());
            if (!saved.getStorageBucket().equals(storage.getBucketName())) throw new IOException("训练数据集对象存储桶配置已改变");
            local = temporary("dataset-download-", ".zip");
            try (InputStream input = storage.getObjectContent(saved.getObjectKey()); OutputStream output = Files.newOutputStream(local)) {
                verify(input, output, saved.getFileSize(), saved.getSha256());
            }
            return local;
        } catch (Exception ex) {
            if (local != null) Files.deleteIfExists(local);
            if (ex instanceof IOException) throw (IOException) ex;
            throw new IOException("训练数据包读取失败", ex);
        }
    }

    private void build(Path target, Long datasetId, Long versionId, Integer versionNumber, String tenant, DatasetSnapshot snapshot,
                       List<AnnotationImage> samples, TrainingDatasetLayout layout, String yaml, String id, String owner) throws IOException {
        Map<Long, List<AnnotationInstance>> annotations = snapshot.getInstances().stream().collect(Collectors.groupingBy(AnnotationInstance::getImageId));
        List<Map<String, Object>> manifest = new ArrayList<>();
        List<String> calibration = new ArrayList<>();
        Map<String, String> hashes = new HashMap<>();
        try (ZipOutputStream zip = new BoundedZipOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            for (String directory : layout.directories()) entry(zip, directory + "/", new byte[0]);
            for (AnnotationImage sample : samples) {
                renew(tenant, id, owner);
                byte[] bytes;
                try (InputStream input = media.read(sample.getLocalPath()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[32768]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if ((long) output.size() + count > IMAGE_LIMIT) throw new IOException("训练图片超过 25 MiB");
                        output.write(buffer, 0, count);
                    }
                    bytes = output.toByteArray();
                }
                SampleMediaInspector.Inspection inspection = inspector.inspect(sample.getOriginalName(), bytes);
                if (!"image".equals(inspection.getMediaType()) || (sample.getFileSize() != null && sample.getFileSize() != bytes.length)
                    || (hasText(sample.getContentSha256()) && !sample.getContentSha256().equalsIgnoreCase(inspection.getSha256()))
                    || (sample.getMediaWidth() != null && sample.getMediaWidth() > 0 && sample.getMediaWidth() != inspection.getWidth())
                    || (sample.getMediaHeight() != null && sample.getMediaHeight() > 0 && sample.getMediaHeight() != inspection.getHeight()))
                    throw new IOException("训练图片内容、尺寸或大小与冻结版本不一致");
                checkHash(hashes, inspection.getSha256(), sample.getDatasetSplit());
                String extension = sample.getOriginalName().substring(sample.getOriginalName().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                List<AnnotationInstance> labels = annotations.get(sample.getId());
                String image = layout.imagePath(sample.getId(), extension, sample.getDatasetSplit(), labels);
                entry(zip, image, bytes);
                for (Map.Entry<String, byte[]> file : layout.annotations(sample.getId(), sample.getDatasetSplit(), labels, inspection.getWidth(), inspection.getHeight()).entrySet()) entry(zip, file.getKey(), file.getValue());
                manifest.add(layout.sample(sample.getId(), image, sample.getDatasetSplit(), inspection.getWidth(), inspection.getHeight(), inspection.getSha256()));
                if ("train".equals(sample.getDatasetSplit()) && calibration.size() < 20) calibration.add(image);
            }
            text(zip, "dataset.yaml", yaml);
            text(zip, "vls-dataset.json", layout.manifest(datasetId, tenant, manifest));
            text(zip, "coco_subset_20.txt", String.join("\n", calibration));
            text(zip, "version.json", json.writeValueAsString(DataManagementService.map("versionId", versionId.toString(), "versionNumber", versionNumber,
                "datasetId", datasetId.toString(), "annotationType", snapshot.getAnnotationType(), "train", samples.stream().filter(s -> "train".equals(s.getDatasetSplit())).count(),
                "val", samples.stream().filter(s -> "val".equals(s.getDatasetSplit())).count())));
        }
    }

    private List<AnnotationImage> members(DatasetSnapshot snapshot) {
        if (snapshot == null || snapshot.getSamples() == null || snapshot.getInstances() == null || snapshot.getLabels() == null) throw new ServiceException("训练版本快照不完整");
        Set<Long> annotated = snapshot.getInstances().stream().map(AnnotationInstance::getImageId).collect(Collectors.toSet());
        if (snapshot.getSamples().stream().anyMatch(s -> s == null || s.getId() == null)) throw new ServiceException("训练快照包含无效图片编号");
        List<AnnotationImage> result = snapshot.getSamples().stream().filter(DatasetPartitioner::usable).filter(s -> annotated.contains(s.getId()))
            .sorted(Comparator.comparing(AnnotationImage::getId)).collect(Collectors.toList());
        if (result.stream().anyMatch(s -> !Arrays.asList("train", "val").contains(s.getDatasetSplit()))
            || result.stream().noneMatch(s -> "train".equals(s.getDatasetSplit())) || result.stream().noneMatch(s -> "val".equals(s.getDatasetSplit())))
            throw new ServiceException("请完成标注和互斥训练/验证划分，两个集合均需有效样本");
        Set<Long> ids = new HashSet<>(); Map<String, String> hashes = new HashMap<>();
        for (AnnotationImage sample : result) {
            if (sample.getId() == null || !ids.add(sample.getId())) throw new ServiceException("训练快照包含无效或重复图片编号");
            checkHash(hashes, sample.getContentSha256(), sample.getDatasetSplit());
        }
        return result;
    }

    private void renew(String tenant, String id, String owner) throws IOException {
        if (jdbc.update("UPDATE vls_training_dataset_artifact SET lease_until=DATE_ADD(NOW(),INTERVAL 6 HOUR) WHERE tenant_id=? AND id=? AND lease_owner=? AND storage_state='BUILDING' AND lease_until>=NOW()",
            tenant, id, owner) != 1) throw new IOException("训练数据集归档租约已失效");
    }

    private TrainingDatasetArtifact find(String tenant, Long datasetId, Long versionId) {
        List<TrainingDatasetArtifact> rows = jdbc.query("SELECT " + COLUMNS + " FROM vls_training_dataset_artifact WHERE tenant_id=? AND dataset_id=? AND version_id=?", mapper(), tenant, datasetId, versionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    protected OssClient client(String config) { return config == null ? OssFactory.instance() : OssFactory.instance(config); }

    private Path temporary(String prefix, String suffix) throws IOException {
        Path directory = Paths.get(tempDir); Files.createDirectories(directory); return Files.createTempFile(directory, prefix, suffix);
    }

    private static void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name); entry.setTime(0); zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry();
    }
    private static void text(ZipOutputStream zip, String name, String value) throws IOException { entry(zip, name, value.getBytes(StandardCharsets.UTF_8)); }
    private static void checkHash(Map<String, String> hashes, String sha, String split) {
        if (!hasText(sha)) return;
        String previous = hashes.putIfAbsent(sha.toLowerCase(Locale.ROOT), split);
        if (previous != null && !previous.equals(split)) throw new ServiceException("相同图片内容不能同时出现在训练和验证集合中");
    }
    private static String referenceId(String ref) {
        if (!isReference(ref)) throw new ServiceException("训练数据集引用无效");
        return reference(ref.substring(PREFIX.length())).substring(PREFIX.length());
    }
    private static String tenant() {
        String tenant = TenantContextHolder.getTenantId();
        if (!hasText(tenant)) throw new ServiceException("缺少训练数据集租户上下文");
        return tenant;
    }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(); for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 255)); return result.toString();
    }
    private static String hash(byte[] bytes) { return hex(sha256().digest(bytes)); }
    private static void verify(InputStream input, OutputStream output, long expectedSize, String expectedHash) throws IOException {
        MessageDigest hash = sha256(); long size = 0; byte[] buffer = new byte[65536]; int count;
        while ((count = input.read(buffer)) != -1) {
            size += count; if (size > expectedSize) throw new IOException("训练数据包大小校验失败");
            hash.update(buffer, 0, count); if (output != null) output.write(buffer, 0, count);
        }
        if (size != expectedSize || !hex(hash.digest()).equalsIgnoreCase(expectedHash)) throw new IOException("训练数据包大小或 SHA-256 校验失败");
    }
    /** The same expansion limits are enforced when materializing on a compute instance. */
    private static final class BoundedZipOutputStream extends ZipOutputStream {
        private long total;
        private long current;
        private int entries;
        private BoundedZipOutputStream(OutputStream out) { super(out); }
        @Override public void putNextEntry(ZipEntry entry) throws IOException {
            if (++entries > 100000) throw new IOException("训练数据包文件数超过 100000");
            current = 0; super.putNextEntry(entry);
        }
        @Override public void write(byte[] bytes, int offset, int count) throws IOException {
            current += count; total += count;
            if (current > 128L * 1024 * 1024 || total > 32L * 1024 * 1024 * 1024) throw new IOException("训练数据包单项超过 128 MiB 或展开总量超过 32 GiB");
            super.write(bytes, offset, count);
        }
    }
    private static final class BuildWork {
        private final TrainingDatasetArtifact artifact;
        private DatasetVersion saved;
        private DatasetSnapshot snapshot;
        private List<AnnotationImage> samples;
        private TrainingDatasetLayout layout;
        private String yaml;
        private String owner;
        private BuildWork(TrainingDatasetArtifact artifact) { this.artifact = artifact; }
    }
    private static RowMapper<TrainingDatasetArtifact> mapper() {
        return (rs, row) -> {
            TrainingDatasetArtifact artifact = new TrainingDatasetArtifact();
            artifact.setId(rs.getString("id")); artifact.setTenantId(rs.getString("tenant_id")); artifact.setDatasetId(rs.getLong("dataset_id"));
            artifact.setVersionId(rs.getLong("version_id")); artifact.setAnnotationType(rs.getString("annotation_type")); artifact.setState(rs.getString("storage_state"));
            artifact.setSnapshotSha256(rs.getString("snapshot_sha256")); artifact.setDatasetYaml(rs.getString("dataset_yaml"));
            artifact.setStorageConfig(rs.getString("storage_config")); artifact.setStorageBucket(rs.getString("storage_bucket")); artifact.setObjectKey(rs.getString("object_key"));
            long size = rs.getLong("file_size"); artifact.setFileSize(rs.wasNull() ? null : size); artifact.setSha256(rs.getString("sha256")); return artifact;
        };
    }
}
