package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import java.util.*;

/** One validated payload contract for human labels, predictions, dataset versions and training. */
public final class AnnotationPayloads {
    public static final int MAX_IMAGE_PAYLOAD = 24 * 1024 * 1024;
    private AnnotationPayloads() { }

    public static void validate(String kind, List<SmartAnnotationRequests.Box> regions, Integer width, Integer height) {
        AnnotationTaskType task = AnnotationTaskType.of(kind);
        if (regions == null || regions.isEmpty() || regions.size() > 1000) throw new ServiceException("请完成标注后确认；没有目标的图片可以跳过");
        if (width == null || height == null) throw new ServiceException("图片尺寸缺失");
        AnnotationMask.checkDimensions(width, height);
        if (task == AnnotationTaskType.DETECTION) { SmartAnnotationService.validateBoxes(regions, width, height); return; }
        if (task == AnnotationTaskType.CLASSIFICATION && regions.size() != 1) throw new ServiceException("单标签分类每张图片必须且只能选择一个类别");
        BitSet semantic = task == AnnotationTaskType.SEMANTIC_SEGMENTATION ? new BitSet(width * height) : null;
        long encodedSize = 0, decodedPixels = 0;
        for (SmartAnnotationRequests.Box region : regions) {
            if (region.getLabelId() == null || region.getLabelId() <= 0) throw new ServiceException("请为所有标注匹配数据集标签");
            Double confidence = region.getConfidence();
            if (confidence != null && (!Double.isFinite(confidence) || confidence < 0 || confidence > 1)) throw new ServiceException("置信度必须介于0和1之间");
            if (task == AnnotationTaskType.CLASSIFICATION) {
                if (region.getMaskData() != null || region.getX() != null || region.getY() != null || region.getWidth() != null || region.getHeight() != null)
                    throw new ServiceException("图像分类只接受整图类别，不接受框或掩膜");
                continue;
            }
            int x = integer(region.getX()), y = integer(region.getY()), w = integer(region.getWidth()), h = integer(region.getHeight());
            if (x < 0 || y < 0 || w <= 0 || h <= 0 || (long) x + w > width || (long) y + h > height) throw new ServiceException("掩膜区域超出图片范围");
            encodedSize += region.getMaskData() == null ? 0 : region.getMaskData().length(); decodedPixels += (long) w * h;
            if (encodedSize > MAX_IMAGE_PAYLOAD || decodedPixels > 200_000_000L) throw new ServiceException("单图掩膜数据量超过处理限制");
            BitSet pixels = AnnotationMask.decode(region.getMaskData(), w, h);
            if (pixels.isEmpty()) throw new ServiceException("标注掩膜为空，请移除该区域或补充像素");
            if (semantic != null) {
                for (int index = pixels.nextSetBit(0); index >= 0; index = pixels.nextSetBit(index + 1)) {
                    int position = (y + index / w) * width + x + index % w;
                    if (semantic.get(position)) throw new ServiceException("语义分割中一个像素只能属于一个类别");
                    semantic.set(position);
                }
            }
        }
        if (semantic != null && semantic.cardinality() != width * height) throw new ServiceException("语义分割仍有未分配类别的像素，请填充剩余区域后确认");
    }

    private static int integer(Double value) {
        if (value == null || !Double.isFinite(value) || value != Math.rint(value) || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
            throw new ServiceException("像素掩膜坐标和尺寸必须为整数");
        return value.intValue();
    }

    public static AlgorithmAnnotationTypeEnum geometry(String kind) {
        AnnotationTaskType task = AnnotationTaskType.of(kind);
        return task == AnnotationTaskType.CLASSIFICATION ? AlgorithmAnnotationTypeEnum.classification
            : task.isMask() ? AlgorithmAnnotationTypeEnum.mask : AlgorithmAnnotationTypeEnum.rect;
    }

    public static Map<String, Object> content(String kind, SmartAnnotationRequests.Box region) {
        Map<String, Object> content = new LinkedHashMap<>(); content.put("kind", kind);
        if (AnnotationTaskType.of(kind) != AnnotationTaskType.CLASSIFICATION) {
            content.put("x", region.getX()); content.put("y", region.getY()); content.put("width", region.getWidth()); content.put("height", region.getHeight());
            if (AnnotationTaskType.of(kind).isMask()) { content.put("maskEncoding", "png-base64"); content.put("maskData", region.getMaskData()); }
            else content.put("rotation", 0);
        }
        return content;
    }

    public static List<SmartAnnotationRequests.Box> read(String kind, List<AnnotationInstance> instances, ObjectMapper json) {
        List<SmartAnnotationRequests.Box> result = new ArrayList<>();
        for (AnnotationInstance instance : instances) {
            try {
                JsonNode value = json.readTree(instance.getAnnotationData());
                SmartAnnotationRequests.Box region = new SmartAnnotationRequests.Box(); region.setLabelId(instance.getLabelId());
                region.setConfidence(instance.getConfidence() == null ? null : instance.getConfidence().doubleValue());
                if (AnnotationTaskType.of(kind) == AnnotationTaskType.CLASSIFICATION) {
                    if (instance.getAnnotationType() != AlgorithmAnnotationTypeEnum.classification) throw new ServiceException("历史标注不是整图类别，请重新确认分类");
                } else if (instance.getAnnotationType() == AlgorithmAnnotationTypeEnum.circle && AnnotationTaskType.of(kind) == AnnotationTaskType.DETECTION) {
                    double radius = value.path("r").asDouble(-1);
                    region.setX(value.path("cx").asDouble() - radius); region.setY(value.path("cy").asDouble() - radius);
                    region.setWidth(2 * radius); region.setHeight(2 * radius);
                } else {
                    if (instance.getAnnotationType() != geometry(kind)) throw new ServiceException("历史标注格式与当前任务不匹配，请在编辑器中重新确认");
                    region.setX(number(value, "x")); region.setY(number(value, "y")); region.setWidth(number(value, "width")); region.setHeight(number(value, "height"));
                    if (AnnotationTaskType.of(kind).isMask()) region.setMaskData(value.path("maskData").asText(null));
                }
                result.add(region);
            } catch (ServiceException ex) { throw ex; }
            catch (Exception ex) { throw new ServiceException("无法读取历史标注数据"); }
        }
        return result;
    }

    private static Double number(JsonNode node, String name) { return node.has(name) && node.get(name).isNumber() ? node.get(name).asDouble() : null; }

    public static List<Map<String, Object>> summary(List<SmartAnnotationRequests.Box> regions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SmartAnnotationRequests.Box region : regions) result.add(DataManagementService.map("labelId", region.getLabelId() == null ? null : region.getLabelId().toString(),
            "className", region.getClassName(), "confidence", region.getConfidence(), "x", region.getX(), "y", region.getY(), "width", region.getWidth(), "height", region.getHeight()));
        return result;
    }
}
