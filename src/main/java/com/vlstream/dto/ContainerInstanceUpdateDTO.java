package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Container Instance Update DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ContainerInstanceUpdateDTO", description = "Container instance update parameters")
public class ContainerInstanceUpdateDTO {

    @ApiModelProperty(value = "Container instance ID", required = true)
    @NotNull(message = "Container instance ID cannot be empty")
    private Long id;

    @ApiModelProperty(value = "Instance name")
    private String instanceName;

    @ApiModelProperty(value = "Container ID")
    private String containerId;

    @ApiModelProperty(value = "Instance status: running-running, stopped-stopped, error-error, starting-starting, stopping-stopping")
    private String instanceStatus;

    @ApiModelProperty(value = "Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown")
    private String healthStatus;

    @ApiModelProperty(value = "Restart count")
    private Integer restartCount;

    @ApiModelProperty(value = "CPU usage")
    private BigDecimal cpuUsage;

    @ApiModelProperty(value = "Memory usage")
    private BigDecimal memoryUsage;

    @ApiModelProperty(value = "GPU usage")
    private BigDecimal gpuUsage;

    @ApiModelProperty(value = "CPU limit")
    private String cpuLimit;

    @ApiModelProperty(value = "Memory limit")
    private String memoryLimit;

    @ApiModelProperty(value = "GPU limit")
    private String gpuLimit;

    @ApiModelProperty(value = "Port configuration (JSON format)")
    private String portConfig;

    @ApiModelProperty(value = "Environment variable configuration (JSON format)")
    private String envConfig;

    @ApiModelProperty(value = "Volume configuration (JSON format)")
    private String volumeConfig;

    @ApiModelProperty(value = "Logs path")
    private String logsPath;
} 