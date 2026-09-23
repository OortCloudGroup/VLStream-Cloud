package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Resolves the training run's dataset, never the dataset's current editable pointer. */
@Service
public class ModelClassFileService {
    private static final int MAX_BYTES = 1024 * 1024;

    @Resource
    private RemoteModelArtifactService artifactService;

    @Resource
    private ModelClassSnapshotStore snapshotStore;

    public ClassFile prepare(AlgorithmTraining training) throws IOException {
        String ptPath = artifactService.resolvePath(training, "pt");
        if (snapshotStore != null) {
            ClassFile snapshot = snapshotStore.find(training, ptPath);
            if (snapshot != null) {
                validate(snapshot.getContent());
                if (!sha256(snapshot.getContent().getBytes(StandardCharsets.UTF_8)).equals(snapshot.getSha256())) {
                    throw new IOException("模型类别快照校验失败");
                }
                return snapshot;
            }
        }
        int weights = ptPath.lastIndexOf("/weights/");
        if (weights < 1) {
            throw new IOException("无法定位模型训练目录，不能确认类别文件来源");
        }
        Map<?, ?> args = parse(read(ptPath.substring(0, weights) + "/args.yaml"));
        Object data = args.containsKey("vls_dataset_yaml") ? args.get("vls_dataset_yaml") : args.get("data");
        if (!(data instanceof String) || !((String) data).startsWith("/")
            || !((String) data).matches("(?s).+\\.ya?ml")) {
            throw new IOException("训练 args.yaml 缺少绝对路径的 data YAML，不能确认类别文件来源");
        }
        String path = (String) data;
        String content = read(path);
        validate(content);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new ClassFile(path.substring(path.lastIndexOf('/') + 1), content,
            bytes.length, sha256(bytes));
    }

    private String read(String path) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        artifactService.stream(path, new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                if (bytes.size() >= MAX_BYTES) throw new IOException("类别元数据文件超过 1 MiB");
                bytes.write(value);
            }

            @Override
            public void write(byte[] value, int offset, int length) throws IOException {
                if (length > MAX_BYTES - bytes.size()) throw new IOException("类别元数据文件超过 1 MiB");
                bytes.write(value, offset, length);
            }
        });
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
    }

    static void validate(String content) throws IOException {
        Map<?, ?> yaml = parse(content);
        Object names = yaml.get("names");
        int count;
        if (names instanceof List) {
            List<?> list = (List<?>) names;
            count = list.size();
            for (Object name : list) validateName(name);
        } else if (names instanceof Map) {
            TreeMap<Integer, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) names).entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!key.matches("0|[1-9][0-9]{0,8}")) throw new IOException("类别编号必须是从 0 开始的连续整数");
                if (sorted.put(Integer.valueOf(key), entry.getValue()) != null) throw new IOException("类别编号重复");
                validateName(entry.getValue());
            }
            count = sorted.size();
            if (count > 0 && (sorted.firstKey() != 0 || sorted.lastKey() != count - 1)) {
                throw new IOException("类别编号必须是从 0 开始的连续整数");
            }
        } else {
            throw new IOException("类别 YAML 缺少 names 列表或映射");
        }
        if (count == 0) throw new IOException("类别 YAML 的 names 不能为空");
        Object nc = yaml.get("nc");
        if (nc != null && !String.valueOf(count).equals(String.valueOf(nc))) {
            throw new IOException("类别 YAML 的 nc 与 names 数量不一致");
        }
    }

    private static void validateName(Object name) throws IOException {
        if (!(name instanceof String) || ((String) name).trim().isEmpty()) {
            throw new IOException("类别名称必须是非空字符串，不能跳过或重新编号");
        }
    }

    private static Map<?, ?> parse(String content) throws IOException {
        try {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(0);
            options.setCodePointLimit(MAX_BYTES);
            DumperOptions dumper = new DumperOptions();
            Object value = new Yaml(new SafeConstructor(options), new Representer(dumper), dumper, options).load(content);
            if (!(value instanceof Map)) throw new IOException("无效的训练或类别 YAML");
            return (Map<?, ?>) value;
        } catch (RuntimeException ex) {
            throw new IOException("训练或类别 YAML 解析失败", ex);
        }
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 unavailable", ex);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ClassFile {
        private final String fileName;
        private final String content;
        private final long fileSize;
        private final String sha256;
    }
}
