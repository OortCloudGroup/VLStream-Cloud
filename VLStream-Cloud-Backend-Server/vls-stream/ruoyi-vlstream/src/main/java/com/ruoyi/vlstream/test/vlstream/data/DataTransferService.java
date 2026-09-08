package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static com.ruoyi.vlstream.test.vlstream.data.DataManagementService.map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataTransferService {
    private final DataManagementService data;
    private final DataMediaStorage storage;
    private final SampleMediaInspector inspector;
    private final ObjectMapper json;
    @Value("${vlstream.data-management.temp-directory:#{systemProperties['java.io.tmpdir'] + '/vls-data'}}")
    private String tempDirectory;

    public Map<String, Object> upload(Long projectId, MultipartFile[] files, String source) {
        data.project(projectId);
        if (files == null || files.length == 0 || files.length > 100) throw new ServiceException("每次支持导入 1 至 100 个文件");
        if (source == null || source.trim().isEmpty() || source.length() > 128) throw new ServiceException("来源不能为空且不能超过 128 字符");
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            try {
                filename = safeName(filename);
                if (file.getSize() > SampleMediaInspector.MAX_FILE_BYTES) throw new ServiceException("单文件不能超过 100 MB");
                Map<String, Object> result = importFile(projectId, filename, file.getBytes(), source.trim());
                result.put("filename", filename); result.put("success", true); results.add(result);
            } catch (IOException | ServiceException e) {
                results.add(map("filename", filename, "success", false, "message", e.getMessage()));
            } catch (RuntimeException e) {
                log.warn("Sample import failed: projectId={}, filename={}", projectId, filename, e);
                results.add(map("filename", filename, "success", false, "message", "文件存储或登记失败，请重试"));
            }
        }
        return map("results", results, "successCount", results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count());
    }

    private Map<String, Object> importFile(Long projectId, String filename, byte[] bytes, String source) {
        SampleMediaInspector.Inspection inspection = inspector.inspect(filename, bytes);
        String suffix = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String key = "samples/" + data.tenant() + "/" + projectId + "/" + UUID.randomUUID() + suffix;
        storage.put(key, bytes, inspection.getContentType());
        try {
            Map<String, Object> result = data.register(projectId, filename, key, bytes.length, source, inspection);
            if (Boolean.TRUE.equals(result.get("duplicate"))) cleanupObject(key);
            return result;
        } catch (RuntimeException e) {
            cleanupObject(key); throw e;
        }
    }

    private void cleanupObject(String key) {
        try { storage.remove(key); }
        catch (RuntimeException e) { log.warn("Could not clean unregistered sample object: {}", key); }
    }

    public Map<String, Object> inspect(Long projectId, List<Long> ids) {
        List<AnnotationImage> samples = data.requireSelection(projectId, ids);
        DatasetSnapshot snapshot = data.snapshot(projectId);
        List<Map<String, Object>> results = new ArrayList<>();
        for (AnnotationImage sample : samples) {
            String failure = null;
            SampleMediaInspector.Inspection inspection = null;
            try (InputStream input = storage.read(sample.getLocalPath())) {
                inspection = inspector.inspect(sample.getOriginalName(), readLimited(input, SampleMediaInspector.MAX_FILE_BYTES));
                List<AnnotationInstance> annotations = new ArrayList<>();
                snapshot.getInstances().stream().filter(i -> Objects.equals(sample.getId(), i.getImageId())).forEach(annotations::add);
                String issue = annotationIssue(annotations, inspection.getMediaType());
                if (!issue.isEmpty()) inspection.setIssues(inspection.getIssues().isEmpty() ? issue : inspection.getIssues() + "；" + issue);
            } catch (IOException | ServiceException e) { failure = e.getMessage(); }
            catch (RuntimeException e) { failure = "文件无法读取，请检查对象存储或文件是否缺失"; }
            data.saveInspection(projectId, sample.getId(), inspection, failure);
            results.add(map("id", String.valueOf(sample.getId()), "filename", sample.getImageName(), "success", failure == null,
                "issues", failure == null ? inspection.getIssues() : failure));
        }
        return map("results", results);
    }

    private String annotationIssue(List<AnnotationInstance> annotations, String mediaType) {
        if ("video".equals(mediaType)) return "视频原始素材不直接参与图片训练划分";
        if (annotations.isEmpty()) return "尚未标注";
        for (AnnotationInstance instance : annotations) {
            try {
                JsonNode shape = json.readTree(instance.getAnnotationData());
                if (shape == null || !shape.isObject() || shape.isEmpty()) return "标注数据为空或格式无效";
                if (instance.getLabelId() == null) return "标注缺少类别";
                if (instance.getAnnotationType() != null && "rect".equals(instance.getAnnotationType().name())) {
                    for (String key : Arrays.asList("x", "y", "width", "height"))
                        if (!shape.has(key) || !shape.get(key).isNumber()) return "矩形标注缺少有效坐标";
                    if (shape.get("width").asDouble() <= 0 || shape.get("height").asDouble() <= 0
                        || shape.get("x").asDouble() < 0 || shape.get("y").asDouble() < 0) return "矩形标注范围无效";
                }
            } catch (Exception e) { return "标注数据无法解析"; }
        }
        return "";
    }

    /** Materialize the complete archive first, so a missing object cannot return a truncated HTTP 200 ZIP. */
    public Path exportArchive(DatasetSnapshot snapshot) throws IOException {
        Path archive = tempFile("export-", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            long total = 0;
            for (AnnotationImage sample : snapshot.getSamples()) {
                String entry = archivePath(sample);
                zip.putNextEntry(new ZipEntry(entry));
                try (InputStream input = storage.read(sample.getLocalPath())) {
                    byte[] buffer = new byte[32768]; int count; long fileBytes = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count; fileBytes += count;
                        if (total > 1024L * 1024 * 1024) throw new ServiceException("单次导出上限为 1 GB，请缩小筛选范围");
                        zip.write(buffer, 0, count);
                    }
                    if (sample.getFileSize() != null && fileBytes != sample.getFileSize()) throw new ServiceException("样本文件大小异常：" + sample.getImageName());
                }
                zip.closeEntry();
                sample.setLocalPath(entry); // The archive manifest contains relative paths, never signed URLs.
            }
            zip.putNextEntry(new ZipEntry("vls-samples.json"));
            zip.write(data.writeJson(snapshot).getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            return archive;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(archive); throw e;
        }
    }

    public Map<String, Object> importArchive(Long projectId, MultipartFile file) throws IOException {
        data.project(projectId);
        if (file.isEmpty() || file.getSize() > 500L * 1024 * 1024) throw new ServiceException("ZIP 文件不能为空且不能超过 500 MB");
        Path directory = Files.createTempDirectory(tempRoot(), "import-");
        try {
            Map<String, Path> entries = new LinkedHashMap<>(); long total = 0;
            try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String name = entry.getName().replace('\\', '/');
                    Path target = directory.resolve(name).normalize();
                    if (!target.startsWith(directory) || name.startsWith("/") || name.contains(":") || name.contains("../") || entries.containsKey(name))
                        throw new ServiceException("ZIP 包含非法或重复路径");
                    if (entries.size() >= 501) throw new ServiceException("单次 ZIP 导入最多 500 个样本");
                    Files.createDirectories(target.getParent());
                    try (OutputStream output = Files.newOutputStream(target)) {
                        byte[] buffer = new byte[32768]; int count; long bytes = 0;
                        while ((count = zip.read(buffer)) != -1) {
                            bytes += count; total += count;
                            if (bytes > SampleMediaInspector.MAX_FILE_BYTES || total > 1024L * 1024 * 1024) throw new ServiceException("ZIP 解压后超过单文件 100 MB 或总计 1 GB 限制");
                            output.write(buffer, 0, count);
                        }
                    }
                    entries.put(name, target);
                }
            }
            DatasetSnapshot manifest = null;
            if (entries.containsKey("vls-samples.json")) {
                Path manifestFile = entries.remove("vls-samples.json");
                if (Files.size(manifestFile) > 20 * 1024 * 1024) throw new ServiceException("样本清单过大");
                manifest = data.readSnapshot(new String(Files.readAllBytes(manifestFile), StandardCharsets.UTF_8));
                validateManifest(projectId, manifest, entries);
            }
            if (entries.isEmpty()) throw new ServiceException("ZIP 内没有可导入文件");
            // Validate all files and hashes before the first storage/database write.
            Map<String, SampleMediaInspector.Inspection> inspections = new LinkedHashMap<>();
            for (Map.Entry<String, Path> entry : entries.entrySet()) inspections.put(entry.getKey(), inspector.inspect(safeName(entry.getKey()), Files.readAllBytes(entry.getValue())));
            if (manifest != null) {
                for (AnnotationImage sample : manifest.getSamples()) {
                    if (DataManagementService.hasText(sample.getContentSha256()) && !sample.getContentSha256().equals(inspections.get(sample.getLocalPath()).getSha256()))
                        throw new ServiceException("归档文件校验和不一致：" + sample.getImageName());
                }
            }
            List<PendingSampleImport> staged = new ArrayList<>();
            try {
                for (Map.Entry<String, Path> entry : entries.entrySet()) {
                    String name = safeName(entry.getKey());
                    String key = "samples/" + data.tenant() + "/" + projectId + "/" + UUID.randomUUID() + name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
                    staged.add(new PendingSampleImport(entry.getKey(), name, key, Files.size(entry.getValue()), inspections.get(entry.getKey())));
                    storage.put(key, Files.readAllBytes(entry.getValue()), inspections.get(entry.getKey()).getContentType());
                }
                Map<String, Object> report = data.registerArchive(projectId, staged, manifest);
                List<?> results = (List<?>) report.get("results");
                for (int i = 0; i < staged.size(); i++) {
                    if (Boolean.TRUE.equals(((Map<?, ?>) results.get(i)).get("duplicate"))) cleanupObject(staged.get(i).getObjectKey());
                }
                return report;
            } catch (IOException | RuntimeException e) {
                staged.forEach(item -> cleanupObject(item.getObjectKey())); throw e;
            }
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
            }
        }
    }

    private void validateManifest(Long projectId, DatasetSnapshot manifest, Map<String, Path> entries) {
        if (!Objects.equals(data.project(projectId).getAnnotationType(), manifest.getAnnotationType())) throw new ServiceException("归档标注类型与当前项目不一致");
        if (manifest.getSamples().size() != entries.size() || manifest.getLabels().size() > 1000 || manifest.getInstances().size() > 100000)
            throw new ServiceException("样本清单数量不匹配或标注数量超限");
        Set<Long> sampleIds = new HashSet<>(), labelIds = new HashSet<>(); Set<String> paths = new HashSet<>();
        manifest.getLabels().forEach(label -> {
            if (label.getId() == null || !labelIds.add(label.getId()) || !DataManagementService.hasText(label.getName()) || label.getName().length() > 50
                || (label.getColor() != null && label.getColor().length() > 20) || (label.getDescription() != null && label.getDescription().length() > 4000))
                throw new ServiceException("清单类别无效或重复");
        });
        for (AnnotationImage sample : manifest.getSamples()) {
            if (sample.getId() == null || !sampleIds.add(sample.getId()) || !entries.containsKey(sample.getLocalPath()) || !paths.add(sample.getLocalPath()))
                throw new ServiceException("清单样本 ID 或文件路径无效/重复");
            if (!DataManagementService.hasText(sample.getImageName()) || sample.getImageName().length() > 200
                || sample.getImageName().contains("/") || sample.getImageName().contains("\\") || sample.getImageName().contains(".."))
                throw new ServiceException("清单样本名称无效");
            if (data.tags(sample).size() > 30 || data.tags(sample).stream().anyMatch(tag -> tag == null || tag.length() > 64)
                || (sample.getSampleSource() != null && sample.getSampleSource().length() > 128)) throw new ServiceException("清单标签或来源长度超限");
        }
        for (AnnotationInstance instance : manifest.getInstances()) {
            if (!sampleIds.contains(instance.getImageId()) || !labelIds.contains(instance.getLabelId()) || instance.getAnnotationType() == null
                || instance.getAnnotationData() == null || instance.getAnnotationData().length() > 100000) throw new ServiceException("清单标注引用或内容无效");
            try { if (!json.readTree(instance.getAnnotationData()).isObject()) throw new IllegalArgumentException(); }
            catch (Exception e) { throw new ServiceException("清单标注必须为有效 JSON 对象"); }
        }
    }

    public Path tempFile(String prefix, String suffix) throws IOException { return Files.createTempFile(tempRoot(), prefix, suffix); }

    private Path tempRoot() throws IOException { return Files.createDirectories(Paths.get(tempDirectory).toAbsolutePath().normalize()); }

    private static String archivePath(AnnotationImage sample) {
        return "media/" + sample.getId() + "/" + safeName(sample.getOriginalName());
    }

    static String safeName(String value) {
        if (value == null || value.trim().isEmpty()) throw new ServiceException("文件名不能为空");
        String normalized = value.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[<>:\"|?*\\p{Cntrl}]", "_");
        if (normalized.isEmpty() || normalized.length() > 200 || normalized.equals(".") || normalized.equals("..")) throw new ServiceException("文件名无效或超过 200 字符");
        return normalized;
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[32768]; int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new ServiceException("样本文件超过检查大小限制");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
