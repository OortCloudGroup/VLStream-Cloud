package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 算法训练任务统计DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingStatisticsDTO", description = "算法训练任务统计信息")
public class AlgorithmTrainingStatisticsDTO {

    @ApiModelProperty(value = "总任务数")
    private Long totalTasks;

    @ApiModelProperty(value = "等待中的任务数")
    private Long pendingTasks;

    @ApiModelProperty(value = "训练中的任务数")
    private Long trainingTasks;

    @ApiModelProperty(value = "已完成的任务数")
    private Long completedTasks;

    @ApiModelProperty(value = "失败的任务数")
    private Long failedTasks;

    @ApiModelProperty(value = "成功率")
    private BigDecimal successRate;

    @ApiModelProperty(value = "平均准确率")
    private BigDecimal averageAccuracy;

    @ApiModelProperty(value = "平均精确率")
    private BigDecimal averagePrecision;

    @ApiModelProperty(value = "平均召回率")
    private BigDecimal averageRecall;

    @ApiModelProperty(value = "平均mAP值")
    private BigDecimal averageMap;

    @ApiModelProperty(value = "平均训练时长（分钟）")
    private BigDecimal averageDuration;

    @ApiModelProperty(value = "今日新增任务数")
    private Long todayNewTasks;

    @ApiModelProperty(value = "本周新增任务数")
    private Long weeklyNewTasks;

    @ApiModelProperty(value = "本月新增任务数")
    private Long monthlyNewTasks;
} 