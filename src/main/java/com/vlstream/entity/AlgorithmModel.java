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
 * Algorithm Model Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_model")
@ApiModel(value = "AlgorithmModel object", description = "Algorithm model")
public class AlgorithmModel {

    @ApiModelProperty(value = "Model ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Model name")
    @TableField("model_name")
    private String modelName;

    @ApiModelProperty(value = "Algorithm ID")
    @TableField("algorithm_id")
    private Long algorithmId;

    @ApiModelProperty(value = "Training task ID")
    @TableField("training_id")
    private Long trainingId;

    @ApiModelProperty(value = "Model version")
    @TableField("version")
    private Integer version;

    @ApiModelProperty(value = "Model format: ONNX, PyTorch, TensorFlow")
    @TableField("model_format")
    private String modelFormat;

    @ApiModelProperty(value = "Model size")
    @TableField("model_size")
    private String modelSize;

    @ApiModelProperty(value = "Model file path")
    @TableField("model_path")
    private String modelPath;

    @ApiModelProperty(value = "Model accuracy")
    @TableField("accuracy")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "Status: draft-draft, testing-testing, published-published")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "Model description")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "Download count")
    @TableField("download_count")
    private Integer downloadCount;

    @ApiModelProperty(value = "Deployment count")
    @TableField("deploy_count")
    private Integer deployCount;

    @ApiModelProperty(value = "Publish time")
    @TableField("publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

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

    // Virtual fields for associated queries
    @ApiModelProperty(value = "Algorithm name")
    @TableField(exist = false)
    private String algorithmName;

    @ApiModelProperty(value = "Training task name")
    @TableField(exist = false)
    private String trainingTaskName;

    @ApiModelProperty(value = "Creator name")
    @TableField(exist = false)
    private String createdByName;

    @ApiModelProperty(value = "Status description")
    @TableField(exist = false)
    private String statusDesc;

    // Business logic methods

    /**
     * Get status description
     */
    public String getStatusDesc() {
        if (status == null) {
            return "Unknown";
        }
        switch (status) {
            case "draft":
                return "Draft";
            case "testing":
                return "Testing";
            case "published":
                return "Published";
            default:
                return "Unknown";
        }
    }

    /**
     * Check if model can be published
     */
    public boolean canPublish() {
        return "draft".equals(status) || "testing".equals(status);
    }

    /**
     * Check if model can be deleted
     */
    public boolean canDelete() {
        return "draft".equals(status) || "testing".equals(status);
    }

    /**
     * Check if model can be downloaded
     */
    public boolean canDownload() {
        return "published".equals(status);
    }

    /**
     * Check if model can be deployed
     */
    public boolean canDeploy() {
        return "published".equals(status);
    }

    /**
     * Increase download count
     */
    public void increaseDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    /**
     * Increase deployment count
     */
    public void increaseDeployCount() {
        this.deployCount = (this.deployCount == null ? 0 : this.deployCount) + 1;
    }

    /**
     * Get formatted accuracy
     */
    public String getFormattedAccuracy() {
        if (accuracy == null) {
            return "Unknown";
        }
        return accuracy.multiply(new BigDecimal("100")).setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * Get model format description
     */
    public String getModelFormatDesc() {
        if (modelFormat == null) {
            return "Unknown";
        }
        switch (modelFormat.toLowerCase()) {
            case "onnx":
                return "ONNX format";
            case "pytorch":
                return "PyTorch format";
            case "tensorflow":
                return "TensorFlow format";
            default:
                return modelFormat;
        }
    }
} 