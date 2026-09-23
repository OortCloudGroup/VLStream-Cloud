package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native training artifacts for all four tasks; instance masks are never reduced to bounding boxes. */
public final class TrainingDatasetLayout {
    private final AnnotationTaskType type;
    private final Map<Long, Integer> classes = new LinkedHashMap<>();
    private final List<String> names = new ArrayList<>();
    private final ObjectMapper json = new ObjectMapper();
    private final YoloDatasetWriter detection = new YoloDatasetWriter(json);

    public TrainingDatasetLayout(String kind, List<AnnotationLabel> labels) {
        type = AnnotationTaskType.of(kind);
        List<AnnotationLabel> sorted = new ArrayList<>(labels); sorted.sort(Comparator.comparing(AnnotationLabel::getId));
        Set<String> unique = new HashSet<>();
        for (AnnotationLabel label : sorted) {
            if (label.getName() == null || label.getName().trim().isEmpty() || !unique.add(label.getName())) throw new ServiceException("训练标签不能为空或重名");
            classes.put(label.getId(), classes.size()); names.add(label.getName());
        }
        if (names.isEmpty() || names.size() > 65535) throw new ServiceException("训练类别数量无效");
    }

    public List<String> directories() {
        List<String> result = new ArrayList<>(Arrays.asList("images/train", "images/val", "images/predict", "labels/train", "labels/val", "annotations", "masks"));
        if (type == AnnotationTaskType.CLASSIFICATION) for (String split : Arrays.asList("train", "val"))
            for (int index = 0; index < names.size(); index++) result.add("classification/" + split + "/" + classDirectory(index));
        return result;
    }

    public String imagePath(Long id, String extension, String split, List<AnnotationInstance> annotations) {
        if (!Arrays.asList("train", "val", "predict").contains(split) || !Arrays.asList("jpg", "jpeg", "png", "bmp").contains(extension)) throw new ServiceException("训练文件路径参数无效");
        if (type == AnnotationTaskType.CLASSIFICATION && !"predict".equals(split)) {
            if (annotations == null || annotations.size() != 1 || !classes.containsKey(annotations.get(0).getLabelId())) throw new ServiceException("单标签分类每张图片需要一个有效类别");
            return "classification/" + split + "/" + classDirectory(classes.get(annotations.get(0).getLabelId())) + "/" + id + "." + extension;
        }
        return "images/" + split + "/" + id + "." + extension;
    }

    public Map<String, byte[]> annotations(Long id, String split, List<AnnotationInstance> annotations, int width, int height) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        if (type == AnnotationTaskType.DETECTION) {
            files.put("labels/" + split + "/" + id + ".txt", detection.labels(annotations, classes, width, height).getBytes(StandardCharsets.UTF_8));
            return files;
        }
        List<SmartAnnotationRequests.Box> regions = AnnotationPayloads.read(type.getCode(), annotations, json);
        AnnotationPayloads.validate(type.getCode(), regions, width, height);
        List<Map<String, Object>> targets = new ArrayList<>();
        for (SmartAnnotationRequests.Box region : regions) {
            Integer classIndex = classes.get(region.getLabelId());
            if (classIndex == null) throw new ServiceException("标注引用了缺失的类别");
            Map<String, Object> target = AnnotationPayloads.content(type.getCode(), region); target.put("classIndex", classIndex); targets.add(target);
        }
        if (type == AnnotationTaskType.INSTANCE_SEGMENTATION || type == AnnotationTaskType.CLASSIFICATION)
            files.put("annotations/" + id + ".json", encode(targets).getBytes(StandardCharsets.UTF_8));
        if (type == AnnotationTaskType.SEMANTIC_SEGMENTATION) {
            BufferedImage mask = new BufferedImage(width, height, names.size() <= 256 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_USHORT_GRAY);
            for (SmartAnnotationRequests.Box region : regions) {
                int x = region.getX().intValue(), y = region.getY().intValue(), w = region.getWidth().intValue(), h = region.getHeight().intValue();
                BitSet pixels = AnnotationMask.decode(region.getMaskData(), w, h);
                for (int index = pixels.nextSetBit(0); index >= 0; index = pixels.nextSetBit(index + 1))
                    mask.getRaster().setSample(x + index % w, y + index / w, 0, classes.get(region.getLabelId()));
            }
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                if (!ImageIO.write(mask, "png", bytes)) throw new ServiceException("无法输出语义分割标签图");
                files.put("masks/" + id + ".png", bytes.toByteArray());
            } catch (java.io.IOException ex) { throw new ServiceException("无法输出语义分割标签图"); }
        }
        return files;
    }

    public Map<String, Object> sample(Long id, String path, String split, int width, int height, String sha) {
        Map<String, Object> sample = DataManagementService.map("id", id.toString(), "path", path, "split", split, "width", width, "height", height, "sha256", sha);
        if (!"predict".equals(split)) sample.put("target", type == AnnotationTaskType.SEMANTIC_SEGMENTATION ? "masks/" + id + ".png"
            : type == AnnotationTaskType.DETECTION ? "labels/" + split + "/" + id + ".txt" : "annotations/" + id + ".json");
        return sample;
    }

    public String yaml(String root) {
        String prefix = type == AnnotationTaskType.CLASSIFICATION ? "classification" : "images";
        return "path: " + encode(root) + "\ntrain: " + prefix + "/train\nval: " + prefix + "/val\nnc: " + names.size() + "\nnames: " + encode(names)
            + "\nannotation_type: " + type.getCode() + "\nvls_format: 2\n";
    }

    public String manifest(Long datasetId, String tenant, List<Map<String, Object>> samples) {
        return encode(DataManagementService.map("format", 2, "datasetId", datasetId.toString(), "tenantId", tenant, "annotationType", type.getCode(), "names", names, "samples", samples));
    }

    private String classDirectory(int index) { return String.format(Locale.ROOT, "c%06d", index); }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); } catch (Exception ex) { throw new ServiceException("无法生成训练数据格式"); }
    }
}
