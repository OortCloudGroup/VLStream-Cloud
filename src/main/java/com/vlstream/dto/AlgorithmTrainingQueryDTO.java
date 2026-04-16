package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Algorithm Training Task Query DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmTrainingQueryDTO", description = "Algorithm training task query parameters")
public class AlgorithmTrainingQueryDTO {

    @ApiModelProperty(value = "Current page number", example = "1")
    private Integer current = 1;

    @ApiModelProperty(value = "Page size", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "Task name (fuzzy search)")
    private String taskName;

    @ApiModelProperty(value = "Algorithm ID")
    private Long algorithmId;

    @ApiModelProperty(value = "Dataset ID")
    private Long datasetId;

    @ApiModelProperty(value = "Training type")
    private String trainType;

    @ApiModelProperty(value = "Training status")
    private String trainStatus;

    @ApiModelProperty(value = "Creator")
    private Long createdBy;

    @ApiModelProperty(value = "Start time - start range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTimeBegin;

    @ApiModelProperty(value = "Start time - end range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTimeEnd;

    @ApiModelProperty(value = "Creation time - start range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTimeBegin;

    @ApiModelProperty(value = "Creation time - end range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTimeEnd;

    @ApiModelProperty(value = "Sort field", example = "created_time")
    private String orderBy = "created_time";

    @ApiModelProperty(value = "Sort direction", example = "desc")
    private String order = "desc";
} 