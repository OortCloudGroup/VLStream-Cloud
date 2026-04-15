package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

/**
 * 算法训练任务创建DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingCreateDTO", description = "算法训练任务创建参数")
public class AlgorithmTrainingCreateDTO {

    @ApiModelProperty(value = "任务名称", required = true)
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @ApiModelProperty(value = "算法ID", required = true)
    @NotNull(message = "算法ID不能为空")
    private Long algorithmId;

    @ApiModelProperty(value = "数据集ID")
    private Long datasetId;

    @ApiModelProperty(value = "训练类型", required = true)
    @NotBlank(message = "训练类型不能为空")
    private String trainType;

    @ApiModelProperty(value = "总轮次", example = "100")
    @Min(value = 1, message = "总轮次必须大于0")
    @Max(value = 10000, message = "总轮次不能超过10000")
    private Integer epochTotal = 100;

    @ApiModelProperty(value = "预计时间")
    private String estimatedTime;

    @ApiModelProperty(value = "模型输出路径")
    private String modelOutputPath;

    @ApiModelProperty(value = "日志路径")
    private String logPath;

    @ApiModelProperty(value = "训练参数(JSON格式)")
    private String configParams;
} 