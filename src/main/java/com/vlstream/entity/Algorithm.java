package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Algorithm Entity Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm")
public class Algorithm {

    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated algorithm repository ID
     */
    @TableField("repository_id")
    private Long repositoryId;

    /**
     * Algorithm name
     */
    @TableField("name")
    private String name;

    /**
     * Algorithm category (person detection, face recognition, video analysis, etc.)
     */
    @TableField("category")
    private String category;

    /**
     * Algorithm category name
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * Algorithm description
     */
    @TableField("description")
    private String description;

    /**
     * Algorithm image URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * Algorithm version
     */
    @TableField("version")
    private String version;

    /**
     * Model file path
     */
    @TableField("model_file_path")
    private String modelFilePath;

    /**
     * Algorithm configuration parameters (JSON format)
     */
    @TableField("config_params")
    private String configParams;

    /**
     * Input format (image, video, etc.)
     */
    @TableField("input_format")
    private String inputFormat;

    /**
     * Output format (bbox, mask, keypoint, etc.)
     */
    @TableField("output_format")
    private String outputFormat;

    /**
     * Accuracy
     */
    @TableField("accuracy")
    private BigDecimal accuracy;

    /**
     * Processing speed (FPS)
     */
    @TableField("processing_speed")
    private Integer processingSpeed;

    /**
     * Memory usage (MB)
     */
    @TableField("memory_usage")
    private Integer memoryUsage;

    /**
     * GPU required: 0-No, 1-Yes
     */
    @TableField("gpu_required")
    private Integer gpuRequired;

    /**
     * Deployment status: ready-ready, deploying-deploying, deployed-deployed, failed-failed
     */
    @TableField("deploy_status")
    private String deployStatus;

    /**
     * Deployment count
     */
    @TableField("deploy_count")
    private Integer deployCount;

    /**
     * Last deployment time
     */
    @TableField("last_deploy_time")
    private LocalDateTime lastDeployTime;

    /**
     * Creator
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * Creation time
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * Updater
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /**
     * Update time
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * Deleted: 0-Not deleted, 1-Deleted
     */
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
} 