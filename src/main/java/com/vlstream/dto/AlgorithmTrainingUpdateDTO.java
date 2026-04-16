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
 * Algorithm Training Task Update DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingUpdateDTO", description = "Algorithm training task update parameters")
public class AlgorithmTrainingUpdateDTO {

    @ApiModelProperty(value = "Training task ID", required = true)
    @NotNull(message = "Training task ID cannot be empty")
    private Long id;

    @ApiModelProperty(value = "Training status")
    private String trainStatus;

    @ApiModelProperty(value = "Training progress percentage")
    @Min(value = 0, message = "Training progress cannot be less than 0")
    @Max(value = 100, message = "Training progress cannot be greater than 100")
    private Integer progress;

    @ApiModelProperty(value = "Current epoch")
    @Min(value = 0, message = "Current epoch cannot be less than 0")
    private Integer epochCurrent;

    @ApiModelProperty(value = "Accuracy")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "Precision")
    private BigDecimal precisionValue;

    @ApiModelProperty(value = "Recall")
    private BigDecimal recallValue;

    @ApiModelProperty(value = "mAP value")
    private BigDecimal mapValue;

    @ApiModelProperty(value = "Loss value")
    private BigDecimal lossValue;

    @ApiModelProperty(value = "GPU usage")
    private String gpuUsage;

    @ApiModelProperty(value = "Estimated time")
    private String estimatedTime;

    @ApiModelProperty(value = "Model output path")
    private String modelOutputPath;

    @ApiModelProperty(value = "Log path")
    private String logPath;

    @ApiModelProperty(value = "Training parameters (JSON format)")
    private String configParams;

    @ApiModelProperty(value = "Error message")
    private String errorMessage;
} 