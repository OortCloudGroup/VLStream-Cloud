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
 * 容器实例实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("container_instance")
@ApiModel(value = "ContainerInstance对象", description = "容器实例")
public class ContainerInstance {

    @ApiModelProperty(value = "容器实例ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "实例名称")
    @TableField("instance_name")
    private String instanceName;

    @ApiModelProperty(value = "容器ID")
    @TableField("container_id")
    private String containerId;

    @ApiModelProperty(value = "镜像名称")
    @TableField("image_name")
    private String imageName;

    @ApiModelProperty(value = "镜像标签")
    @TableField("image_tag")
    private String imageTag;

    @ApiModelProperty(value = "算法ID")
    @TableField("algorithm_id")
    private Long algorithmId;

    @ApiModelProperty(value = "实例类型")
    @TableField("instance_type")
    private String instanceType;

    @ApiModelProperty(value = "CPU限制")
    @TableField("cpu_limit")
    private String cpuLimit;

    @ApiModelProperty(value = "内存限制")
    @TableField("memory_limit")
    private String memoryLimit;

    @ApiModelProperty(value = "GPU限制")
    @TableField("gpu_limit")
    private String gpuLimit;

    @ApiModelProperty(value = "端口配置")
    @TableField("port_config")
    private String portConfig;

    @ApiModelProperty(value = "环境变量配置")
    @TableField("env_config")
    private String envConfig;

    @ApiModelProperty(value = "存储卷配置")
    @TableField("volume_config")
    private String volumeConfig;

    @ApiModelProperty(value = "实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中")
    @TableField("instance_status")
    private String instanceStatus;

    @ApiModelProperty(value = "健康状态：healthy-健康,unhealthy-不健康,unknown-未知")
    @TableField("health_status")
    private String healthStatus;

    @ApiModelProperty(value = "启动时间")
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "停止时间")
    @TableField("stop_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime stopTime;

    @ApiModelProperty(value = "重启次数")
    @TableField("restart_count")
    private Integer restartCount;

    @ApiModelProperty(value = "CPU使用率")
    @TableField("cpu_usage")
    private BigDecimal cpuUsage;

    @ApiModelProperty(value = "内存使用率")
    @TableField("memory_usage")
    private BigDecimal memoryUsage;

    @ApiModelProperty(value = "GPU使用率")
    @TableField("gpu_usage")
    private BigDecimal gpuUsage;

    @ApiModelProperty(value = "日志路径")
    @TableField("logs_path")
    private String logsPath;

    @ApiModelProperty(value = "创建人")
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    @ApiModelProperty(value = "是否删除：0-否，1-是")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    // 非数据库字段，用于查询时关联显示
    @ApiModelProperty(value = "算法名称")
    @TableField(exist = false)
    private String algorithmName;

    @ApiModelProperty(value = "创建人姓名")
    @TableField(exist = false)
    private String createdByName;

    @ApiModelProperty(value = "实例状态描述")
    @TableField(exist = false)
    private String instanceStatusDesc;

    @ApiModelProperty(value = "健康状态描述")
    @TableField(exist = false)
    private String healthStatusDesc;

    @ApiModelProperty(value = "运行时长（分钟）")
    @TableField(exist = false)
    private Long runtimeMinutes;

    /**
     * 获取实例状态描述
     */
    public String getInstanceStatusDesc() {
        if (instanceStatus == null) {
            return "未知";
        }
        switch (instanceStatus) {
            case "running":
                return "运行中";
            case "stopped":
                return "已停止";
            case "error":
                return "错误";
            case "starting":
                return "启动中";
            case "stopping":
                return "停止中";
            default:
                return "未知";
        }
    }

    /**
     * 获取健康状态描述
     */
    public String getHealthStatusDesc() {
        if (healthStatus == null) {
            return "未知";
        }
        switch (healthStatus) {
            case "healthy":
                return "健康";
            case "unhealthy":
                return "不健康";
            case "unknown":
                return "未知";
            default:
                return "未知";
        }
    }

    /**
     * 计算运行时长
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