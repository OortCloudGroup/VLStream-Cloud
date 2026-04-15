package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 容器实例查询条件DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ContainerInstanceQueryDTO", description = "容器实例查询条件")
public class ContainerInstanceQueryDTO {

    @ApiModelProperty(value = "实例名称")
    private String instanceName;

    @ApiModelProperty(value = "容器ID")
    private String containerId;

    @ApiModelProperty(value = "镜像名称")
    private String imageName;

    @ApiModelProperty(value = "算法ID")
    private Long algorithmId;

    @ApiModelProperty(value = "实例类型")
    private String instanceType;

    @ApiModelProperty(value = "实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中")
    private String instanceStatus;

    @ApiModelProperty(value = "健康状态：healthy-健康,unhealthy-不健康,unknown-未知")
    private String healthStatus;

    @ApiModelProperty(value = "创建人")
    private Long createdBy;

    @ApiModelProperty(value = "创建时间开始")
    private LocalDateTime createdTimeStart;

    @ApiModelProperty(value = "创建时间结束")
    private LocalDateTime createdTimeEnd;

    @ApiModelProperty(value = "启动时间开始")
    private LocalDateTime startTimeStart;

    @ApiModelProperty(value = "启动时间结束")
    private LocalDateTime startTimeEnd;

    @ApiModelProperty(value = "排序字段")
    private String orderBy;

    @ApiModelProperty(value = "排序方式：asc-升序，desc-降序")
    private String order;
} 