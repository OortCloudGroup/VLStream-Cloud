package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Persisted algorithm orchestration definition. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_algorithm_orchestration")
public class AlgorithmOrchestration extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private String orchestrationName;
    private String orchestrationDesc;
    private String triggerType;
    private String executeMode;
    private String algorithmSteps;
    private String inputConfig;
    private String outputConfig;
    private Integer deviceCount;
    private Integer runCount;
    private String orchestrationStatus;
    private LocalDateTime lastRunTime;
    private Integer avgRunTime;
    private BigDecimal successRate;
}
