package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Algorithm Training Task Statistics DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingStatisticsDTO", description = "Algorithm training task statistics information")
public class AlgorithmTrainingStatisticsDTO {

    @ApiModelProperty(value = "Total tasks")
    private Long totalTasks;

    @ApiModelProperty(value = "Pending tasks")
    private Long pendingTasks;

    @ApiModelProperty(value = "Training tasks")
    private Long trainingTasks;

    @ApiModelProperty(value = "Completed tasks")
    private Long completedTasks;

    @ApiModelProperty(value = "Failed tasks")
    private Long failedTasks;

    @ApiModelProperty(value = "Success rate")
    private BigDecimal successRate;

    @ApiModelProperty(value = "Average accuracy")
    private BigDecimal averageAccuracy;

    @ApiModelProperty(value = "Average precision")
    private BigDecimal averagePrecision;

    @ApiModelProperty(value = "Average recall")
    private BigDecimal averageRecall;

    @ApiModelProperty(value = "Average mAP value")
    private BigDecimal averageMap;

    @ApiModelProperty(value = "Average training duration (minutes)")
    private BigDecimal averageDuration;

    @ApiModelProperty(value = "Today's new tasks")
    private Long todayNewTasks;

    @ApiModelProperty(value = "Weekly new tasks")
    private Long weeklyNewTasks;

    @ApiModelProperty(value = "Monthly new tasks")
    private Long monthlyNewTasks;
} 