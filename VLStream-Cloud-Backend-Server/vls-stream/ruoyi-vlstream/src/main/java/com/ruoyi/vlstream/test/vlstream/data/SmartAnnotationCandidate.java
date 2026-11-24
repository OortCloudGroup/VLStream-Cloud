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
    static final String PILOT = "PILOT";
    static final String SPOT_CHECK = "SPOT_CHECK";
    static final String DEFERRED = "DEFERRED";

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
    private String llmReviewKind;
    @com.fasterxml.jackson.annotation.JsonIgnore private String llmInputSha256;
    @com.fasterxml.jackson.annotation.JsonIgnore private String llmClaimToken;
    @com.fasterxml.jackson.annotation.JsonIgnore private java.util.Date llmClaimUntil;
}
