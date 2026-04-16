package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Container Instance Query Conditions DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ContainerInstanceQueryDTO", description = "Container instance query conditions")
public class ContainerInstanceQueryDTO {

    @ApiModelProperty(value = "Instance name")
    private String instanceName;

    @ApiModelProperty(value = "Container ID")
    private String containerId;

    @ApiModelProperty(value = "Image name")
    private String imageName;

    @ApiModelProperty(value = "Algorithm ID")
    private Long algorithmId;

    @ApiModelProperty(value = "Instance type")
    private String instanceType;

    @ApiModelProperty(value = "Instance status: running-running, stopped-stopped, error-error, starting-starting, stopping-stopping")
    private String instanceStatus;

    @ApiModelProperty(value = "Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown")
    private String healthStatus;

    @ApiModelProperty(value = "Creator")
    private Long createdBy;

    @ApiModelProperty(value = "Creation time start")
    private LocalDateTime createdTimeStart;

    @ApiModelProperty(value = "Creation time end")
    private LocalDateTime createdTimeEnd;

    @ApiModelProperty(value = "Start time start")
    private LocalDateTime startTimeStart;

    @ApiModelProperty(value = "Start time end")
    private LocalDateTime startTimeEnd;

    @ApiModelProperty(value = "Sort field")
    private String orderBy;

    @ApiModelProperty(value = "Sort order: asc-ascending, desc-descending")
    private String order;
} 