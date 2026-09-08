package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import lombok.Data;
import java.util.List;

@Data
public class DatasetSnapshot {
    private int schemaVersion = 1;
    private String annotationType;
    private String annotationRules;
    private List<AnnotationImage> samples;
    private List<AnnotationLabel> labels;
    private List<AnnotationInstance> instances;
}
