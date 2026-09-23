package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationInstanceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/** Shared manual editor supplies valid seed annotations for all four intelligent annotation tasks. */
@Service
@RequiredArgsConstructor
public class DatasetAnnotationEditor {
    private final DataManagementService data;
    private final DataMediaStorage storage;
    private final IVlsAnnotationInstanceService annotations;
    private final ObjectMapper json;
    private final SampleMediaInspector inspector;

    @Data
    public static class Edit extends SmartAnnotationRequests.Review {
        @NotBlank private String revision;
        private boolean replaceLegacy;
    }

    public Map<String, Object> read(Long datasetId, Long sampleId) {
        DatasetSnapshot snapshot = data.snapshot(datasetId);
        AnnotationImage image = data.sample(datasetId, sampleId);
        List<AnnotationInstance> own = snapshot.getInstances().stream().filter(instance -> sampleId.equals(instance.getImageId())).collect(Collectors.toList());
        int[] dimensions = dimensions(image);
        List<SmartAnnotationRequests.Box> regions; String legacyError = null;
        try { regions = AnnotationPayloads.read(snapshot.getAnnotationType(), own, json); }
        catch (ServiceException incompatible) { regions = Collections.emptyList(); legacyError = incompatible.getMessage(); }
        return DataManagementService.map("annotationType", snapshot.getAnnotationType(), "imageId", sampleId.toString(), "imageName", image.getImageName(),
            "previewUrl", storage.preview(image.getLocalPath()), "width", dimensions[0], "height", dimensions[1], "regions", regions,
            "labels", snapshot.getLabels(), "revision", revision(image, own), "legacyError", legacyError);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Long datasetId, Long sampleId, Edit request) {
        data.lockDatasetForTask(datasetId);
        DatasetSnapshot snapshot = data.snapshot(datasetId); AnnotationImage image = data.sample(datasetId, sampleId);
        List<AnnotationInstance> own = snapshot.getInstances().stream().filter(instance -> sampleId.equals(instance.getImageId())).collect(Collectors.toList());
        if (!revision(image, own).equals(request.getRevision())) throw new ServiceException("图片或标注已被其他操作修改，请重新载入后编辑");
        boolean legacy = false;
        try { AnnotationPayloads.read(snapshot.getAnnotationType(), own, json); } catch (ServiceException incompatible) { legacy = true; }
        if (legacy) {
            if (!request.isReplaceLegacy()) throw new ServiceException("请先确认重新标注历史数据");
            DataRequests.Version backup = new DataRequests.Version(); backup.setName("历史标注格式备份 · " + image.getImageName().substring(0, Math.min(60, image.getImageName().length())));
            backup.setDescription("用户确认替换旧标注格式前自动保存"); data.saveVersion(datasetId, backup);
        }
        int[] dimensions = dimensions(image);
        if (!request.getBoxes().isEmpty()) AnnotationPayloads.validate(snapshot.getAnnotationType(), request.getBoxes(), dimensions[0], dimensions[1]);
        data.validateAnnotationOwner(datasetId, sampleId, request.getBoxes().stream().map(SmartAnnotationRequests.Box::getLabelId).collect(Collectors.toList()));
        List<AnnotationInstance> saved = new ArrayList<>();
        for (SmartAnnotationRequests.Box region : request.getBoxes()) {
            AnnotationInstance instance = new AnnotationInstance(); instance.setTenantId(data.tenant()); instance.setAnnotationId(datasetId); instance.setImageId(sampleId);
            instance.setLabelId(region.getLabelId()); instance.setAnnotationType(AnnotationPayloads.geometry(snapshot.getAnnotationType()));
            instance.setAnnotationData(data.writeJson(AnnotationPayloads.content(snapshot.getAnnotationType(), region))); instance.setVerified(1); instance.setConfidence(BigDecimal.ONE); saved.add(instance);
        }
        annotations.batchSaveAnnotations(datasetId, sampleId, saved);
    }

    private int[] dimensions(AnnotationImage image) {
        if ("video".equals(image.getMediaType())) throw new ServiceException("视频需先切图后再标注");
        if (image.getMediaWidth() != null && image.getMediaHeight() != null && image.getMediaWidth() > 0 && image.getMediaHeight() > 0)
            return new int[]{image.getMediaWidth(), image.getMediaHeight()};
        try (InputStream input = storage.read(image.getLocalPath())) {
            SampleMediaInspector.Inspection inspection = inspector.inspect(image.getOriginalName(), SmartAnnotationWorker.limited(input, 25 * 1024 * 1024));
            return new int[]{inspection.getWidth(), inspection.getHeight()};
        } catch (Exception ex) { throw new ServiceException("无法读取图片尺寸，请检查样本文件"); }
    }

    private String revision(AnnotationImage image, List<AnnotationInstance> instances) {
        try {
            List<String> content = instances.stream().sorted(Comparator.comparing(AnnotationInstance::getId))
                .map(item -> item.getId() + ":" + item.getLabelId() + ":" + item.getAnnotationType() + ":" + item.getAnnotationData()).collect(Collectors.toList());
            byte[] hash = MessageDigest.getInstance("SHA-256").digest((image.getLocalPath() + ":" + image.getContentSha256() + ":" + String.join("\n", content)).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) { throw new ServiceException("无法生成标注版本"); }
    }
}
