package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 算法训练任务查询DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingQueryDTO", description = "算法训练任务查询参数")
public class AlgorithmTrainingQueryDTO {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer current = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "任务名称（模糊搜索）")
    private String taskName;

    @ApiModelProperty(value = "算法ID")
    private Long algorithmId;

    @ApiModelProperty(value = "数据集ID")
    private Long datasetId;

    @ApiModelProperty(value = "训练类型")
    private String trainType;

    @ApiModelProperty(value = "训练状态")
    private String trainStatus;

    @ApiModelProperty(value = "创建人")
    private Long createdBy;

    @ApiModelProperty(value = "开始时间-开始范围")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTimeBegin;

    @ApiModelProperty(value = "开始时间-结束范围")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTimeEnd;

    @ApiModelProperty(value = "创建时间-开始范围")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTimeBegin;

    @ApiModelProperty(value = "创建时间-结束范围")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTimeEnd;

    @ApiModelProperty(value = "排序字段", example = "created_time")
    private String orderBy = "created_time";

    @ApiModelProperty(value = "排序方式", example = "desc")
    private String order = "desc";
} 