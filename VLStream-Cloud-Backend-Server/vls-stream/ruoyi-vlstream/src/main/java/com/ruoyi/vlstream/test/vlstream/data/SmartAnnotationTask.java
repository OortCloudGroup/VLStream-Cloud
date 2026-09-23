package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_smart_annotation_task")
public class SmartAnnotationTask extends TenantEntity {
    @JsonSerialize(using = ToStringSerializer.class) private Long datasetId;
    private String taskName;
    private String mode;
    private String annotationType = "object_detection";
    private String sourceType;
    @JsonSerialize(using = ToStringSerializer.class) private Long sourceId;
    private String modelName;
    @JsonIgnore private String modelPath;
    private String taskState;
    private Integer roundNumber;
    private Double confidence;
    private Integer epochs;
    private Integer reviewSize;
    private String errorMessage;
    @JsonIgnore private Long bulkCursor;
    private Integer bulkTotal;
    private Integer bulkProcessed;
    private Integer bulkAccepted;
    private Integer bulkConflicts;
    private String bulkError;
}
