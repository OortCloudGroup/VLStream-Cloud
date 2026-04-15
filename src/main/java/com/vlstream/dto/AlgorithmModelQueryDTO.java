package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 算法模型查询DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmModelQueryDTO", description = "算法模型查询参数")
public class AlgorithmModelQueryDTO {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer current = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "模型名称（模糊搜索）")
    private String modelName;

    @ApiModelProperty(value = "算法ID")
    private Long algorithmId;

    @ApiModelProperty(value = "训练任务ID")
    private Long trainingId;

    @ApiModelProperty(value = "模型版本")
    private String version;

    @ApiModelProperty(value = "模型格式")
    private String modelFormat;

    @ApiModelProperty(value = "模型状态")
    private String status;

    @ApiModelProperty(value = "创建人")
    private Long createdBy;

    @ApiModelProperty(value = "最小准确率")
    private BigDecimal minAccuracy;

    @ApiModelProperty(value = "最大准确率")
    private BigDecimal maxAccuracy;

    @ApiModelProperty(value = "发布时间-开始范围")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTimeBegin;

    @ApiModelProperty(value = "发布时间-结束范围")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTimeEnd;

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