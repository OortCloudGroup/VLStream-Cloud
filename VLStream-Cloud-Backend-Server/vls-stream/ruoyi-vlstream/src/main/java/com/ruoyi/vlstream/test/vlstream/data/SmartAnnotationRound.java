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
@TableName("vls_smart_annotation_round")
public class SmartAnnotationRound extends TenantEntity {
    @JsonSerialize(using = ToStringSerializer.class) private Long taskId;
    private Integer roundNumber;
    @JsonSerialize(using = ToStringSerializer.class) private Long versionId;
    private String roundState;
    @JsonIgnore private String workDirectory;
    @JsonIgnore private String modelPath;
    private String modelSha256;
    private Integer predictedCount;
    private String progressStage;
    private Integer progressCurrent;
    private Integer progressTotal;
    private String errorMessage;
}
