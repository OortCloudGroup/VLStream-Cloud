package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/** Reads outside database locks, reports all problematic images, and prepares a hash-aware snapshot. */
@Service
@RequiredArgsConstructor
public class DatasetGenerationPreflight {
    private final DataMediaStorage media;
    private final SampleMediaInspector inspector;
    private final ObjectMapper json;

    public DatasetGenerationReport check(Long id, DatasetSnapshot current) throws IOException {
        DatasetGenerationReport report = new DatasetGenerationReport();
        report.setDatasetId(id.toString()); report.setOriginalJson(json.writeValueAsString(current));
        DatasetSnapshot prepared = json.readValue(report.getOriginalJson(), DatasetSnapshot.class);
        report.setPrepared(prepared);
        Map<Long, List<AnnotationInstance>> annotations = prepared.getInstances().stream().collect(Collectors.groupingBy(AnnotationInstance::getImageId));
        YoloDatasetWriter writer = new YoloDatasetWriter(json);
        TrainingDatasetLayout layout = new TrainingDatasetLayout(prepared.getAnnotationType(), prepared.getLabels());
        List<AnnotationImage> eligible = prepared.getSamples().stream().filter(DatasetPartitioner::usable)
            .filter(s -> annotations.containsKey(s.getId())).collect(Collectors.toList());
        for (AnnotationImage sample : eligible) {
            report.setCheckedImages(report.getCheckedImages() + 1);
            try {
                byte[] bytes;
                try (InputStream input = media.read(sample.getLocalPath()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[32768]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if ((long) output.size() + count > 25 * 1024 * 1024) throw new ServiceException("图片超过25 MiB");
                        output.write(buffer, 0, count);
                    }
                    bytes = output.toByteArray();
                }
                SampleMediaInspector.Inspection image = inspector.inspect(sample.getOriginalName(), bytes);
                if (!"image".equals(image.getMediaType())) throw new ServiceException("视频需先切图");
                if ((sample.getFileSize() != null && sample.getFileSize() != bytes.length)
                    || (sample.getContentSha256() != null && !sample.getContentSha256().isEmpty() && !sample.getContentSha256().equalsIgnoreCase(image.getSha256()))
                    || (sample.getMediaWidth() != null && sample.getMediaWidth() > 0 && sample.getMediaWidth() != image.getWidth())
                    || (sample.getMediaHeight() != null && sample.getMediaHeight() > 0 && sample.getMediaHeight() != image.getHeight()))
                    throw new ServiceException("图片内容、大小或尺寸与样本记录不一致");
                sample.setContentSha256(image.getSha256()); sample.setMediaWidth(image.getWidth()); sample.setMediaHeight(image.getHeight());
                List<AnnotationInstance> values = annotations.get(sample.getId());
                if ("object_detection".equals(prepared.getAnnotationType())) {
                    for (AnnotationInstance instance : values) {
                        try {
                            double[] original = writer.box(instance, image.getWidth(), image.getHeight());
                            double[] clipped = YoloDatasetWriter.intersect(original[0], original[1], original[2], original[3], image.getWidth(), image.getHeight());
                            boolean changed = false;
                            for (int i = 0; i < 4; i++) changed |= Math.abs(original[i] - clipped[i]) > 1e-8;
                            if (changed) report.getCorrections().add(issue(sample, instance, "训练包按图片边界裁剪，原标注保留", original, clipped));
                        } catch (ServiceException ex) { report.getErrors().add(issue(sample, instance, ex.getMessage(), null, null)); }
                    }
                }
                // Validate categories, geometry and classification membership through the same export writer.
                layout.imagePath(sample.getId(), sample.getOriginalName().substring(sample.getOriginalName().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT), "train", values);
                layout.annotations(sample.getId(), "train", values, image.getWidth(), image.getHeight());
            } catch (Exception ex) {
                if (report.getErrors().stream().noneMatch(e -> sample.getId().toString().equals(e.get("imageId"))))
                    report.getErrors().add(issue(sample, null, ex instanceof ServiceException ? ex.getMessage() : "图片读取或解码失败，请检查原始文件", null, null));
            }
        }
        if (!report.getErrors().isEmpty()) { report.setStatus("BLOCKED"); return report; }
        Map<String, String> hashes = new HashMap<>(); boolean needsSplit = false;
        for (AnnotationImage sample : eligible) {
            String split = sample.getDatasetSplit();
            if (!Arrays.asList("train", "val").contains(split)) needsSplit = true;
            String previous = hashes.putIfAbsent(sample.getContentSha256(), split);
            if (previous != null && !Objects.equals(previous, split)) needsSplit = true;
        }
        needsSplit |= eligible.stream().noneMatch(s -> "train".equals(s.getDatasetSplit())) || eligible.stream().noneMatch(s -> "val".equals(s.getDatasetSplit()));
        if (needsSplit) {
            Map<Long, String> classes = new HashMap<>();
            annotations.forEach((imageId, values) -> classes.put(imageId, values.get(0).getLabelId().toString()));
            DataRequests.Split request = new DataRequests.Split();
            if ("image_classification".equals(prepared.getAnnotationType())) request.setMode("stratified");
            Map<Long, String> partition = DatasetPartitioner.partition(eligible, classes, request);
            eligible.forEach(s -> s.setDatasetSplit(partition.get(s.getId())));
            report.setRepartitioned(true);
        }
        return report;
    }

    private Map<String, Object> issue(AnnotationImage sample, AnnotationInstance instance, String reason, double[] original, double[] clipped) {
        Map<String, Object> row = DataManagementService.map("imageId", sample.getId().toString(), "name", sample.getOriginalName(), "reason", reason);
        if (instance != null && instance.getId() != null) row.put("annotationId", instance.getId().toString());
        if (original != null) row.put("originalBox", original);
        if (clipped != null) row.put("exportBox", clipped);
        try { row.put("previewUrl", media.preview(sample.getLocalPath())); } catch (Exception ignored) { /* Issues remain actionable without preview. */ }
        return row;
    }
}
