package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.common.enums.YesNoEnum;
import com.ruoyi.vlstream.test.vlstream.data.AnnotationTaskType;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Stores an externally trained PT and its exact class mapping as one model version. */
@Service
@RequiredArgsConstructor
public class ImportedAlgorithmModelService {
    private static final long MAX_PT_BYTES = 500L * 1024 * 1024;
    private static final long MAX_YAML_BYTES = 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 32;

    private final VlsAlgorithmMapper algorithms;
    private final IVlsAlgorithmModelService models;
    private final ModelArtifactObjectStore storage;

    @Value("${vlstream.model-storage.temp-dir:${java.io.tmpdir}/vls-model-storage}")
    private String tempDir;

    public AlgorithmModel importModel(Long algorithmId, String annotationType, String modelName, Integer version,
                                      String description, MultipartFile pt, MultipartFile dataYaml) throws IOException {
        validateFile(pt, ".pt", MAX_PT_BYTES);
        validateYamlFile(dataYaml);
        return persist(algorithmId, annotationType, modelName, version, description,
            pt.getOriginalFilename(), readYaml(dataYaml.getBytes()), pt::getInputStream);
    }

    public AlgorithmModel importArchive(Long algorithmId, String annotationType, String modelName, Integer version,
                                        String description, MultipartFile archive) throws IOException {
        validateFile(archive, ".zip", MAX_PT_BYTES);
        Path root = Paths.get(tempDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path extractedPt = Files.createTempFile(root, "model-import-", ".pt");
        try {
            String ptName = null;
            byte[] yamlBytes = null;
            int entries = 0;
            try (ZipInputStream zip = new ZipInputStream(archive.getInputStream())) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entries > MAX_ZIP_ENTRIES) throw new ServiceException("ZIP 文件条目过多");
                    String name = safeZipPath(entry.getName());
                    if (entry.isDirectory()) continue;
                    String baseName = name.substring(name.lastIndexOf('/') + 1);
                    if (baseName.toLowerCase(Locale.ROOT).endsWith(".pt")) {
                        if (ptName != null) throw new ServiceException("ZIP 只能包含一份 PT 模型");
                        ptName = baseName;
                        try (OutputStream output = Files.newOutputStream(extractedPt)) {
                            copyLimited(zip, output, MAX_PT_BYTES);
                        }
                    } else if (isYaml(baseName)) {
                        if (yamlBytes != null) throw new ServiceException("ZIP 只能包含一份类别 YAML");
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        copyLimited(zip, output, MAX_YAML_BYTES);
                        yamlBytes = output.toByteArray();
                    } else {
                        throw new ServiceException("ZIP 只能包含一份 .pt 和一份类别 YAML");
                    }
                    zip.closeEntry();
                }
            }
            if (ptName == null || Files.size(extractedPt) == 0 || yamlBytes == null || yamlBytes.length == 0) {
                throw new ServiceException("ZIP 必须包含一份非空 .pt 和一份非空类别 YAML");
            }
            return persist(algorithmId, annotationType, modelName, version, description,
                ptName, readYaml(yamlBytes), () -> Files.newInputStream(extractedPt));
        } finally {
            Files.deleteIfExists(extractedPt);
        }
    }

    private AlgorithmModel persist(Long algorithmId, String annotationType, String modelName, Integer version,
                                   String description, String originalName, String yaml, InputSupplier pt) throws IOException {
        String tenant = ModelArtifactObjectStore.tenant();
        if (algorithmId == null) throw new ServiceException("请选择所属算法");
        AnnotationTaskType task = AnnotationTaskType.of(annotationType);
        Algorithm algorithm = algorithms.selectOne(Wrappers.<Algorithm>lambdaQuery()
            .eq(Algorithm::getId, algorithmId).eq(Algorithm::getTenantId, tenant)
            .eq(Algorithm::getIsDeleted, 0));
        if (algorithm == null) throw new ServiceException("算法不存在或不属于当前租户");
        if (!matchesCategory(task, algorithm.getCategory())) throw new ServiceException("模型类型与所属算法分类不一致");
        if (modelName == null || modelName.trim().isEmpty() || modelName.trim().length() > 100) {
            throw new ServiceException("模型名称不能为空且不能超过 100 字符");
        }
        if (version == null || version < 1) throw new ServiceException("模型版本必须为正整数");
        if (description != null && description.trim().length() > 200) {
            throw new ServiceException("模型描述不能超过 200 字符");
        }
        if (models.count(Wrappers.<AlgorithmModel>lambdaQuery()
            .eq(AlgorithmModel::getTenantId, tenant).eq(AlgorithmModel::getModelName, modelName.trim())
            .eq(AlgorithmModel::getVersion, version).eq(AlgorithmModel::getIsDeleted, 0)) > 0) {
            throw new ServiceException("当前租户已存在同名同版本模型");
        }
        ModelClassFileService.validate(yaml);
        ModelClassFileService.validateAnnotationType(yaml, task.getCode());

        String fileName = ModelArtifactObjectStore.safeName(originalName);
        if (fileName.length() > 200) throw new ServiceException("模型文件名不能超过 200 字符");
        String path = "imports/" + UUID.randomUUID() + "/weights/" + fileName;
        String classPath = ModelClassFileService.storagePath(path);
        if (!storage.archive(classPath, "data.yaml", target -> Files.write(target, yaml.getBytes(StandardCharsets.UTF_8)))) {
            throw new IOException("类别配置正在保存，请稍后重试");
        }
        if (!storage.archive(path, fileName, target -> {
            try (InputStream input = pt.open()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
        })) throw new IOException("模型文件正在保存，请稍后重试");

        AlgorithmModel model = new AlgorithmModel();
        model.setTenantId(tenant);
        model.setAlgorithmId(algorithmId);
        model.setModelName(modelName.trim());
        model.setVersion(version);
        model.setModelFormat("pt");
        model.setAnnotationType(task.getCode());
        model.setModelPath(path);
        model.setDescription(description == null ? null : description.trim());
        model.setStatus(YesNoEnum.NO.getCode());
        model.setIsDeleted(0);
        model.setDownloadCount(0);
        model.setDeployCount(0);
        if (!models.save(model)) throw new ServiceException("保存导入模型记录失败");
        return model;
    }

    private static boolean matchesCategory(AnnotationTaskType task, AlgorithmCategoryEnum category) {
        switch (task) {
            case CLASSIFICATION: return category == AlgorithmCategoryEnum.classify;
            case DETECTION: return category == AlgorithmCategoryEnum.detect || category == AlgorithmCategoryEnum.personDetect;
            case INSTANCE_SEGMENTATION: return category == AlgorithmCategoryEnum.segment;
            case SEMANTIC_SEGMENTATION: return category == AlgorithmCategoryEnum.semanticSeg;
            default: return false;
        }
    }

    private static void validateFile(MultipartFile file, String suffix, long maxBytes) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) {
            throw new ServiceException("上传文件为空或超过大小限制：" + suffix);
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            throw new ServiceException("请选择 " + suffix + " 文件");
        }
    }

    private static void validateYamlFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_YAML_BYTES
            || !isYaml(file.getOriginalFilename())) {
            throw new ServiceException("请选择不超过 1 MiB 的 .yaml 或 .yml 类别文件");
        }
    }

    private static boolean isYaml(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }

    private static String safeZipPath(String original) {
        if (original == null || original.length() > 512) throw new ServiceException("ZIP 条目路径无效");
        String name = original.replace('\\', '/');
        if (name.startsWith("/") || name.contains(":") || name.indexOf('\0') >= 0) {
            throw new ServiceException("ZIP 包含非法路径");
        }
        for (String part : name.split("/")) {
            if (part.equals("..") || part.equals(".")) throw new ServiceException("ZIP 包含非法路径");
        }
        return name;
    }

    private static long copyLimited(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int length;
        while ((length = input.read(buffer)) != -1) {
            total += length;
            if (total > limit) throw new ServiceException("ZIP 解压后的文件超过大小限制");
            output.write(buffer, 0, length);
        }
        return total;
    }

    private static String readYaml(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            throw new ServiceException("data.yaml 必须采用 UTF-8 编码");
        }
    }

    @FunctionalInterface
    private interface InputSupplier { InputStream open() throws IOException; }
}
