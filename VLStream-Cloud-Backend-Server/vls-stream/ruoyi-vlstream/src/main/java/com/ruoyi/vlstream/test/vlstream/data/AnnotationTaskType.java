package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import java.util.Arrays;

/** Dataset task semantics are distinct from the geometry stored in each annotation instance. */
public enum AnnotationTaskType {
    CLASSIFICATION("image_classification", "classify", "图像分类"),
    DETECTION("object_detection", "detect", "物体检测"),
    INSTANCE_SEGMENTATION("instance_segmentation", "segment", "实例分割"),
    SEMANTIC_SEGMENTATION("semantic_segmentation", "semanticSeg", "语义分割");

    private final String code;
    private final String modelTask;
    private final String label;

    AnnotationTaskType(String code, String modelTask, String label) {
        this.code = code;
        this.modelTask = modelTask;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getModelTask() { return modelTask; }
    public String getLabel() { return label; }
    public boolean isMask() { return this == INSTANCE_SEGMENTATION || this == SEMANTIC_SEGMENTATION; }

    public static AnnotationTaskType of(String code) {
        return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst()
            .orElseThrow(() -> new ServiceException("不支持的标注任务类型"));
    }
}
