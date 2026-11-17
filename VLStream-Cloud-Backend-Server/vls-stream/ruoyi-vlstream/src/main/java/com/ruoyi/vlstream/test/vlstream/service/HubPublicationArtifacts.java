package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifact;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifactService;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves only tenant-owned, immutable READY artifacts. Never executes training or follows caller URLs. */
@Service
@RequiredArgsConstructor
public class HubPublicationArtifacts {
    static final long MAX_FILE = 100L * 1024 * 1024;
    static final long MAX_TOTAL = 500L * 1024 * 1024;
    private final IVlsAlgorithmModelService models;
    private final VlsAlgorithmMapper algorithms;
    private final ComputePublicTaskAccess access;
    private final ModelArtifactObjectStore store;
    private final DataManagementService data;
    private final TrainingDatasetArtifactService datasets;
    private final ObjectMapper json;

    public Source inspect(String kind, Long id) { return resolve(kind, id, false).source; }

    public Prepared prepare(String kind, Long id) throws IOException {
        Plan plan = resolve(kind, id, true);
        Prepared result = new Prepared(plan.source, Files.createTempDirectory("vls-hub-publication-"));
        try {
            if (plan.dataset != null) {
                Path downloaded = datasets.download(plan.dataset);
                try {
                    Path target = result.directory.resolve("dataset.zip");
                    Files.copy(downloaded, target);
                    verify(target, plan.dataset.getFileSize(), plan.dataset.getSha256());
                    result.files.put("dataset.zip", target);
                } finally { Files.deleteIfExists(downloaded); }
            } else {
                for (Map.Entry<String, ModelArtifactObjectStore.StoredArtifact> entry : plan.artifacts.entrySet()) {
                    Path target = result.directory.resolve(entry.getKey());
                    ModelArtifactObjectStore.StoredArtifact artifact = entry.getValue();
                    try (OutputStream output = limited(Files.newOutputStream(target), artifact.getFileSize())) {
                        store.stream(artifact, output);
                    }
                    verify(target, artifact.getFileSize(), artifact.getSha256());
                    result.files.put(entry.getKey(), target);
                }
                Path classes = result.files.get("classes.yaml");
                if (classes != null) {
                    String content = new String(Files.readAllBytes(classes), StandardCharsets.UTF_8);
                    ModelClassFileService.validate(content);
                    if (StringUtils.isNotBlank(plan.source.modelType))
                        ModelClassFileService.validateAnnotationType(content, plan.source.modelType);
                }
                if (plan.deviceCompatible) {
                    Map<String, Object> manifest = new LinkedHashMap<>();
                    manifest.put("schema_version", 1);
                    manifest.put("model_format", "om");
                    manifest.put("target_chip", "Hi3519DV500");
                    manifest.put("runtime", "svp-acl");
                    manifest.put("algorithm_type", "detection");
                    manifest.put("model", manifestFile("model.om", plan.artifacts.get("model.om")));
                    manifest.put("classes", manifestFile("classes.yaml", plan.artifacts.get("classes.yaml")));
                    Path target = result.directory.resolve("vls-device-manifest.json");
                    Files.write(target, json.writeValueAsBytes(manifest));
                    result.files.put("vls-device-manifest.json", target);
                }
            }
            long total = 0;
            for (Path file : result.files.values()) total += Files.size(file);
            if (total > MAX_TOTAL) throw new IOException("发布文件总量超过 500 MiB");
            return result;
        } catch (Exception ex) {
            try { result.close(); } catch (IOException cleanup) { ex.addSuppressed(cleanup); }
            if (ex instanceof IOException) throw (IOException) ex;
            if (ex instanceof RuntimeException) throw (RuntimeException) ex;
            throw new IOException("准备发布文件失败", ex);
        }
    }

    private Plan resolve(String kind, Long id, boolean strict) {
        if (id == null || id <= 0) throw new ServiceException("资源编号无效");
        String tenant = ModelArtifactObjectStore.tenant();
        Plan plan = new Plan();
        StringBuilder version = new StringBuilder(kind).append(':').append(tenant).append(':').append(id);
        if ("model".equals(kind)) {
            AlgorithmModel model = models.getOne(new QueryWrapper<AlgorithmModel>().eq("id", id)
                .eq("tenant_id", tenant).eq("is_deleted", 0));
            if (model == null) throw new ServiceException("模型不存在或不属于当前租户");
            access.model(id);
            // Imported models have no training record, so their creator must be checked explicitly.
            if (model.getTrainingId() == null && (StringUtils.isBlank(model.getCreateUser())
                || !Objects.equals(LoginHelper.getUserId(), model.getCreateUser())))
                throw new ServiceException("不能发布其他用户导入的模型");
            String type = model.getAnnotationType();
            if (StringUtils.isBlank(type) && model.getAlgorithmId() != null) {
                Algorithm algorithm = algorithms.selectOne(new QueryWrapper<Algorithm>().eq("id", model.getAlgorithmId())
                    .eq("tenant_id", tenant).eq("is_deleted", 0));
                if (algorithm != null) type = annotationType(algorithm.getCategory());
            }
            plan.source = new Source(model.getModelName(), model.getDescription(), category(type), type, null);
            try {
            add(plan, "model.pt", model.getModelPath(), false);
            add(plan, "model.om", model.getOmModelOutputPath(), false);
            if (plan.artifacts.isEmpty()) throw new ServiceException("模型暂无 READY 的 PT 或 OM 归档，不能发布");
            // No mutable current-dataset fallback: classes must belong to this saved PT version.
            if (StringUtils.isNotBlank(model.getModelPath()))
                add(plan, "classes.yaml", ModelClassFileService.storagePath(model.getModelPath()), true);
            if (plan.artifacts.containsKey("classes.yaml") && plan.artifacts.get("classes.yaml").getFileSize() > 1024 * 1024)
                throw new ServiceException("模型类别文件超过 1 MiB");
            plan.deviceCompatible = model.getTrainingId() != null && model.getAlgorithmId() != null
                && "object_detection".equals(type) && plan.artifacts.containsKey("model.om")
                && plan.artifacts.containsKey("classes.yaml");
            version.append(':').append(model.getVersion()).append(':').append(type).append(':').append(plan.deviceCompatible);
            for (Map.Entry<String, ModelArtifactObjectStore.StoredArtifact> item : plan.artifacts.entrySet())
                version.append(':').append(item.getKey()).append(':').append(item.getValue().getFileSize()).append(':').append(item.getValue().getSha256());
            } catch (ServiceException ex) {
                if (strict) throw ex;
                plan.source.publishable = false; plan.source.reason = ex.getMessage(); return plan;
            }
        } else if ("dataset".equals(kind)) {
            AlgorithmAnnotation project = data.project(id);
            plan.source = new Source(project.getAnnotationName(), project.getRemark(), category(project.getAnnotationType()), project.getAnnotationType(), null);
            try {
            TrainingDatasetArtifact artifact = datasets.latestReady(id);
            checkMetadata(artifact.getFileSize(), artifact.getSha256());
            plan.dataset = artifact;
            plan.source = new Source(project.getAnnotationName(), project.getRemark(), category(artifact.getAnnotationType()), artifact.getAnnotationType(), null);
            version.append(':').append(artifact.getId()).append(':').append(artifact.getFileSize()).append(':').append(artifact.getSha256());
            } catch (ServiceException ex) {
                if (strict) throw ex;
                plan.source.publishable = false; plan.source.reason = ex.getMessage(); return plan;
            }
        } else throw new ServiceException("资源类型必须为 model 或 dataset");
        plan.source.version = ModelArtifactObjectStore.hash(version.toString());
        return plan;
    }

    private void add(Plan plan, String name, String path, boolean required) {
        ModelArtifactObjectStore.StoredArtifact artifact = StringUtils.isBlank(path) ? null : store.find(path);
        if (artifact == null) {
            if (required) throw new ServiceException("该固定模型版本的类别文件尚未归档，不能发布");
            return;
        }
        checkMetadata(artifact.getFileSize(), artifact.getSha256());
        plan.artifacts.put(name, artifact);
    }

    static void checkMetadata(Long size, String hash) {
        if (size == null || size <= 0 || size > MAX_FILE) throw new ServiceException("发布文件必须非空且单文件不超过 100 MiB");
        if (hash == null || !hash.matches("[a-f0-9]{64}")) throw new ServiceException("发布文件缺少有效 SHA-256 归档摘要");
    }

    static void verify(Path file, long size, String hash) throws IOException {
        if (Files.size(file) != size || size <= 0 || size > MAX_FILE) throw new IOException("发布文件大小校验失败");
        try (InputStream input = Files.newInputStream(file)) {
            if (!ModelArtifactObjectStore.digest(input).equals(hash)) throw new IOException("发布文件 SHA-256 校验失败");
        }
    }

    static OutputStream limited(OutputStream output, long limit) {
        return new FilterOutputStream(output) {
            private long count;
            @Override public void write(int value) throws IOException {
                if (count >= limit) throw new IOException("发布文件超出归档大小");
                out.write(value); count++;
            }
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                if (length > limit - count) throw new IOException("发布文件超出归档大小");
                out.write(bytes, offset, length); count += length;
            }
        };
    }

    private static Map<String, Object> manifestFile(String path, ModelArtifactObjectStore.StoredArtifact artifact) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("path", path); file.put("sha256", artifact.getSha256()); file.put("size", artifact.getFileSize());
        return file;
    }
    private static String annotationType(AlgorithmCategoryEnum category) {
        if (category == AlgorithmCategoryEnum.detect || category == AlgorithmCategoryEnum.personDetect) return "object_detection";
        if (category == AlgorithmCategoryEnum.classify) return "image_classification";
        if (category == AlgorithmCategoryEnum.segment) return "instance_segmentation";
        if (category == AlgorithmCategoryEnum.semanticSeg) return "semantic_segmentation";
        return null;
    }
    private static String category(String type) {
        if ("object_detection".equals(type)) return "目标检测";
        if ("image_classification".equals(type)) return "图像分类";
        if ("instance_segmentation".equals(type)) return "实例分割";
        if ("semantic_segmentation".equals(type)) return "语义分割";
        return "其他";
    }
    private static class Plan {
        private Source source;
        private TrainingDatasetArtifact dataset;
        private boolean deviceCompatible;
        private final Map<String, ModelArtifactObjectStore.StoredArtifact> artifacts = new LinkedHashMap<>();
    }
    @Getter
    public static class Source {
        private final String name;
        private final String description;
        private final String category;
        private final String modelType;
        private String version;
        private boolean publishable = true;
        private String reason;
        Source(String name, String description, String category, String modelType, String version) {
            this.name = name; this.description = description; this.category = category; this.modelType = modelType; this.version = version;
        }
        public String getSourceName() { return name; }
    }
    public static class Prepared extends Source implements AutoCloseable {
        private final Path directory;
        private final Map<String, Path> files = new LinkedHashMap<>();
        Prepared(Source source, Path directory) {
            super(source.name, source.description, source.category, source.modelType, source.version);
            this.directory = directory;
        }
        public Map<String, Path> getFiles() { return Collections.unmodifiableMap(files); }
        @Override public void close() throws IOException {
            // This directory is created exclusively above, contains only our fixed, flat filenames.
            try (java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
                for (Path entry : entries) Files.deleteIfExists(entry);
            }
            Files.deleteIfExists(directory);
        }
    }
}
