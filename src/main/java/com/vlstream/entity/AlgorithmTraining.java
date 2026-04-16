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
 * Algorithm Training Task Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_training")
@ApiModel(value = "AlgorithmTraining object", description = "Algorithm training task")
public class AlgorithmTraining {

    @ApiModelProperty(value = "Training task ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Task name")
    @TableField("task_name")
    private String taskName;

    @ApiModelProperty(value = "Algorithm ID")
    @TableField("algorithm_id")
    private Long algorithmId;

    @ApiModelProperty(value = "Dataset ID")
    @TableField("dataset_id")
    private Long datasetId;

    @ApiModelProperty(value = "Training status: pending-pending, training-training, completed-completed, failed-failed")
    @TableField("train_status")
    private String trainStatus;

    @ApiModelProperty(value = "Training progress percentage")
    @TableField("progress")
    private Integer progress;

    @ApiModelProperty(value = "Current epoch")
    @TableField("epoch_current")
    private Integer epochCurrent;

    @ApiModelProperty(value = "Total epochs")
    @TableField("epoch_total")
    private Integer epochTotal;

    @ApiModelProperty(value = "Accuracy")
    @TableField("accuracy")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "Precision")
    @TableField("precision_value")
    private BigDecimal precisionValue;

    @ApiModelProperty(value = "Recall")
    @TableField("recall_value")
    private BigDecimal recallValue;

    @ApiModelProperty(value = "mAP value")
    @TableField("map_value")
    private BigDecimal mapValue;

    @ApiModelProperty(value = "Loss value")
    @TableField("loss_value")
    private BigDecimal lossValue;

    @ApiModelProperty(value = "GPU usage")
    @TableField("gpu_usage")
    private String gpuUsage;

    @ApiModelProperty(value = "Start time")
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "End time")
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "Estimated time")
    @TableField("estimated_time")
    private String estimatedTime;

    @ApiModelProperty(value = "Model output path")
    @TableField("model_output_path")
    private String modelOutputPath;

    @ApiModelProperty(value = "Log path")
    @TableField("log_path")
    private String logPath;

    @ApiModelProperty(value = "Training parameters")
    @TableField("config_params")
    private String configParams;

    @ApiModelProperty(value = "Error message")
    @TableField("error_message")
    private String errorMessage;

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

    @ApiModelProperty(value = "Algorithm type")
    @TableField(exist = false)
    private String trainType;

    @ApiModelProperty(value = "Target model")
    @TableField(exist = false)
    private String targetModel;

    @ApiModelProperty(value = "Dataset name")
    @TableField(exist = false)
    private String datasetName;

    @ApiModelProperty(value = "Creator name")
    @TableField(exist = false)
    private String createdByName;

    @ApiModelProperty(value = "Training duration (minutes)")
    @TableField(exist = false)
    private Long durationMinutes;

    @ApiModelProperty(value = "Training status description")
    @TableField(exist = false)
    private String trainStatusDesc;

    // Status constants
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_TRAINING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_STOPPED = 3;
    public static final int STATUS_FAILED = 4;

    // Status field (for compatibility with old code)
    @ApiModelProperty(value = "Status: 0-pending, 1-training, 2-completed, 3-stopped, 4-failed")
    @TableField(exist = false)
    private Integer status;

    /**
     * Get training status description
     */
    public String getTrainStatusDesc() {
        if (trainStatus == null) {
            return "Unknown";
        }
        switch (trainStatus) {
            case "pending":
                return "Pending";
            case "training":
                return "Training";
            case "completed":
                return "Completed";
            case "failed":
                return "Failed";
            default:
                return "Unknown";
        }
    }

    /**
     * Calculate training duration
     */
    public Long getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return null;
    }

    /**
     * Set status
     */
    public void setStatus(Integer status) {
        this.status = status;
        // Synchronously update trainStatus
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
     * Get status
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * Get dataset path
     */
    public String getDatasetPath() {
        // Should query dataset path based on datasetId
        // Temporarily return default path
        return "./datasets/emgitemsv6/emgitemsv6.yaml";
    }

    /**
     * Get base model
     */
    public String getBaseModel() {
        // Should query base model based on algorithmId
        // Temporarily return default model
        return "yolov8n.pt";
    }

    /**
     * Get training parameters
     */
    public String getTrainParams() {
        // Return configured training parameters
        return this.configParams;
    }
} 