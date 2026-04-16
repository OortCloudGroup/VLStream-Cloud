package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Container Instance Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("container_instance")
@ApiModel(value = "ContainerInstance object", description = "Container instance")
public class ContainerInstance {

    @ApiModelProperty(value = "Container instance ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Instance name")
    @TableField("instance_name")
    private String instanceName;

    @ApiModelProperty(value = "Container ID")
    @TableField("container_id")
    private String containerId;

    @ApiModelProperty(value = "Image name")
    @TableField("image_name")
    private String imageName;

    @ApiModelProperty(value = "Image tag")
    @TableField("image_tag")
    private String imageTag;

    @ApiModelProperty(value = "Algorithm ID")
    @TableField("algorithm_id")
    private Long algorithmId;

    @ApiModelProperty(value = "Instance type")
    @TableField("instance_type")
    private String instanceType;

    @ApiModelProperty(value = "CPU limit")
    @TableField("cpu_limit")
    private String cpuLimit;

    @ApiModelProperty(value = "Memory limit")
    @TableField("memory_limit")
    private String memoryLimit;

    @ApiModelProperty(value = "GPU limit")
    @TableField("gpu_limit")
    private String gpuLimit;

    @ApiModelProperty(value = "Port configuration")
    @TableField("port_config")
    private String portConfig;

    @ApiModelProperty(value = "Environment variable configuration")
    @TableField("env_config")
    private String envConfig;

    @ApiModelProperty(value = "Volume configuration")
    @TableField("volume_config")
    private String volumeConfig;

    @ApiModelProperty(value = "Instance status: running-running, stopped-stopped, error-error, starting-starting, stopping-stopping")
    @TableField("instance_status")
    private String instanceStatus;

    @ApiModelProperty(value = "Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown")
    @TableField("health_status")
    private String healthStatus;

    @ApiModelProperty(value = "Start time")
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "Stop time")
    @TableField("stop_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime stopTime;

    @ApiModelProperty(value = "Restart count")
    @TableField("restart_count")
    private Integer restartCount;

    @ApiModelProperty(value = "CPU usage")
    @TableField("cpu_usage")
    private BigDecimal cpuUsage;

    @ApiModelProperty(value = "Memory usage")
    @TableField("memory_usage")
    private BigDecimal memoryUsage;

    @ApiModelProperty(value = "GPU usage")
    @TableField("gpu_usage")
    private BigDecimal gpuUsage;

    @ApiModelProperty(value = "Logs path")
    @TableField("logs_path")
    private String logsPath;

    @ApiModelProperty(value = "Creator")
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "Update time")
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    @ApiModelProperty(value = "Deleted: 0-No, 1-Yes")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    // Non-database fields for associated display in queries
    @ApiModelProperty(value = "Algorithm name")
    @TableField(exist = false)
    private String algorithmName;

    @ApiModelProperty(value = "Creator name")
    @TableField(exist = false)
    private String createdByName;

    @ApiModelProperty(value = "Instance status description")
    @TableField(exist = false)
    private String instanceStatusDesc;

    @ApiModelProperty(value = "Health status description")
    @TableField(exist = false)
    private String healthStatusDesc;

    @ApiModelProperty(value = "Runtime duration (minutes)")
    @TableField(exist = false)
    private Long runtimeMinutes;

    /**
     * Get instance status description
     */
    public String getInstanceStatusDesc() {
        if (instanceStatus == null) {
            return "Unknown";
        }
        switch (instanceStatus) {
            case "running":
                return "Running";
            case "stopped":
                return "Stopped";
            case "error":
                return "Error";
            case "starting":
                return "Starting";
            case "stopping":
                return "Stopping";
            default:
                return "Unknown";
        }
    }

    /**
     * Get health status description
     */
    public String getHealthStatusDesc() {
        if (healthStatus == null) {
            return "Unknown";
        }
        switch (healthStatus) {
            case "healthy":
                return "Healthy";
            case "unhealthy":
                return "Unhealthy";
            case "unknown":
                return "Unknown";
            default:
                return "Unknown";
        }
    }

    /**
     * Calculate runtime duration
     */
    public Long getRuntimeMinutes() {
        if (startTime != null && stopTime == null && "running".equals(instanceStatus)) {
            return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
        } else if (startTime != null && stopTime != null) {
            return java.time.Duration.between(startTime, stopTime).toMinutes();
        }
        return null;
    }
} 