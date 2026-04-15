package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;

/**
 * 算法训练任务更新DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingUpdateDTO", description = "算法训练任务更新参数")
public class AlgorithmTrainingUpdateDTO {

    @ApiModelProperty(value = "训练任务ID", required = true)
    @NotNull(message = "训练任务ID不能为空")
    private Long id;

    @ApiModelProperty(value = "训练状态")
    private String trainStatus;

    @ApiModelProperty(value = "训练进度百分比")
    @Min(value = 0, message = "训练进度不能小于0")
    @Max(value = 100, message = "训练进度不能大于100")
    private Integer progress;

    @ApiModelProperty(value = "当前轮次")
    @Min(value = 0, message = "当前轮次不能小于0")
    private Integer epochCurrent;

    @ApiModelProperty(value = "准确率")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "精确率")
    private BigDecimal precisionValue;

    @ApiModelProperty(value = "召回率")
    private BigDecimal recallValue;

    @ApiModelProperty(value = "mAP值")
    private BigDecimal mapValue;

    @ApiModelProperty(value = "损失值")
    private BigDecimal lossValue;

    @ApiModelProperty(value = "GPU使用率")
    private String gpuUsage;

    @ApiModelProperty(value = "预计时间")
    private String estimatedTime;

    @ApiModelProperty(value = "模型输出路径")
    private String modelOutputPath;

    @ApiModelProperty(value = "日志路径")
    private String logPath;

    @ApiModelProperty(value = "训练参数(JSON格式)")
    private String configParams;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;
} 