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
 * Algorithm Training Task Creation DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingCreateDTO", description = "Algorithm training task creation parameters")
public class AlgorithmTrainingCreateDTO {

    @ApiModelProperty(value = "Task name", required = true)
    @NotBlank(message = "Task name cannot be empty")
    private String taskName;

    @ApiModelProperty(value = "Algorithm ID", required = true)
    @NotNull(message = "Algorithm ID cannot be empty")
    private Long algorithmId;

    @ApiModelProperty(value = "Dataset ID")
    private Long datasetId;

    @ApiModelProperty(value = "Training type", required = true)
    @NotBlank(message = "Training type cannot be empty")
    private String trainType;

    @ApiModelProperty(value = "Total epochs", example = "100")
    @Min(value = 1, message = "Total epochs must be greater than 0")
    @Max(value = 10000, message = "Total epochs cannot exceed 10000")
    private Integer epochTotal = 100;

    @ApiModelProperty(value = "Estimated time")
    private String estimatedTime;

    @ApiModelProperty(value = "Model output path")
    private String modelOutputPath;

    @ApiModelProperty(value = "Log path")
    private String logPath;

    @ApiModelProperty(value = "Training parameters (JSON format)")
    private String configParams;
} 