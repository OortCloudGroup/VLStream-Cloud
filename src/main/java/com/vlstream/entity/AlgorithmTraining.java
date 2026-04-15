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
 * 算法训练任务实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_training")
@ApiModel(value = "AlgorithmTraining对象", description = "算法训练任务")
public class AlgorithmTraining {

    @ApiModelProperty(value = "训练任务ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "任务名称")
    @TableField("task_name")
    private String taskName;

    @ApiModelProperty(value = "算法ID")
    @TableField("algorithm_id")
    private Long algorithmId;

    @ApiModelProperty(value = "数据集ID")
    @TableField("dataset_id")
    private Long datasetId;

    @ApiModelProperty(value = "训练状态：pending-等待,training-训练中,completed-完成,failed-失败")
    @TableField("train_status")
    private String trainStatus;

    @ApiModelProperty(value = "训练进度百分比")
    @TableField("progress")
    private Integer progress;

    @ApiModelProperty(value = "当前轮次")
    @TableField("epoch_current")
    private Integer epochCurrent;

    @ApiModelProperty(value = "总轮次")
    @TableField("epoch_total")
    private Integer epochTotal;

    @ApiModelProperty(value = "准确率")
    @TableField("accuracy")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "精确率")
    @TableField("precision_value")
    private BigDecimal precisionValue;

    @ApiModelProperty(value = "召回率")
    @TableField("recall_value")
    private BigDecimal recallValue;

    @ApiModelProperty(value = "map值")
    @TableField("map_value")
    private BigDecimal mapValue;

    @ApiModelProperty(value = "损失值")
    @TableField("loss_value")
    private BigDecimal lossValue;

    @ApiModelProperty(value = "GPU使用率")
    @TableField("gpu_usage")
    private String gpuUsage;

    @ApiModelProperty(value = "开始时间")
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "预计时间")
    @TableField("estimated_time")
    private String estimatedTime;

    @ApiModelProperty(value = "模型输出路径")
    @TableField("model_output_path")
    private String modelOutputPath;

    @ApiModelProperty(value = "日志路径")
    @TableField("log_path")
    private String logPath;

    @ApiModelProperty(value = "训练参数")
    @TableField("config_params")
    private String configParams;

    @ApiModelProperty(value = "错误信息")
    @TableField("error_message")
    private String errorMessage;

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

    @ApiModelProperty(value = "算法类型")
    @TableField(exist = false)
    private String trainType;

    @ApiModelProperty(value = "对应模型")
    @TableField(exist = false)
    private String targetModel;

    @ApiModelProperty(value = "数据集名称")
    @TableField(exist = false)
    private String datasetName;

    @ApiModelProperty(value = "创建人姓名")
    @TableField(exist = false)
    private String createdByName;

    @ApiModelProperty(value = "训练时长（分钟）")
    @TableField(exist = false)
    private Long durationMinutes;

    @ApiModelProperty(value = "训练状态描述")
    @TableField(exist = false)
    private String trainStatusDesc;

    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_TRAINING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_STOPPED = 3;
    public static final int STATUS_FAILED = 4;

    // 状态字段（用于兼容旧代码）
    @ApiModelProperty(value = "状态：0-等待,1-训练中,2-完成,3-停止,4-失败")
    @TableField(exist = false)
    private Integer status;

    /**
     * 获取训练状态描述
     */
    public String getTrainStatusDesc() {
        if (trainStatus == null) {
            return "未知";
        }
        switch (trainStatus) {
            case "pending":
                return "等待中";
            case "training":
                return "训练中";
            case "completed":
                return "已完成";
            case "failed":
                return "失败";
            default:
                return "未知";
        }
    }

    /**
     * 计算训练时长
     */
    public Long getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return null;
    }

    /**
     * 设置状态
     */
    public void setStatus(Integer status) {
        this.status = status;
        // 同步更新trainStatus
        switch (status) {
            case STATUS_PENDING:
                this.trainStatus = "pending";
                break;
            case STATUS_TRAINING:
                this.trainStatus = "training";
                break;
            case STATUS_COMPLETED:
                this.trainStatus = "completed";
                break;
            case STATUS_STOPPED:
                this.trainStatus = "stopped";
                break;
            case STATUS_FAILED:
                this.trainStatus = "failed";
                break;
        }
    }

    /**
     * 获取状态
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 获取数据集路径
     */
    public String getDatasetPath() {
        // 这里应该根据datasetId查询数据集路径
        // 暂时返回默认路径
        return "./datasets/emgitemsv6/emgitemsv6.yaml";
    }

    /**
     * 获取基础模型
     */
    public String getBaseModel() {
        // 这里应该根据algorithmId查询基础模型
        // 暂时返回默认模型
        return "yolov8n.pt";
    }

    /**
     * 获取训练参数
     */
    public String getTrainParams() {
        // 返回配置的训练参数
        return this.configParams;
    }
} 