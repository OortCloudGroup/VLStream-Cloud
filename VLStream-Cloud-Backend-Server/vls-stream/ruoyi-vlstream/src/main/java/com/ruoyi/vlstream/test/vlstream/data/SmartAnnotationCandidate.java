package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_smart_annotation_candidate")
public class SmartAnnotationCandidate extends TenantEntity {
    @JsonSerialize(using = ToStringSerializer.class) private Long taskId;
    @JsonSerialize(using = ToStringSerializer.class) private Long roundId;
    @JsonSerialize(using = ToStringSerializer.class) private Long imageId;
    private Integer imageWidth;
    private Integer imageHeight;
    private String boxesJson;
    @com.fasterxml.jackson.annotation.JsonIgnore private String summaryJson;
    private String reviewError;
    private Double uncertainty;
    private Integer hardExample;
    private String reviewState;
}
