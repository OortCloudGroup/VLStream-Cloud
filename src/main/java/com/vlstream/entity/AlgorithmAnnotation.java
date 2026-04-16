package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Algorithm Annotation Entity Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_annotation")
public class AlgorithmAnnotation {

    /**
     * Annotation ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Annotation name
     */
    @TableField("annotation_name")
    private String annotationName;

    /**
     * Annotation type: object_detection-object detection, image_classification-image classification, instance_segmentation-instance segmentation, semantic_segmentation-semantic segmentation
     */
    @TableField("annotation_type")
    private String annotationType;

    /**
     * Dataset path
     */
    @TableField("dataset_path")
    private String datasetPath;

    /**
     * Total count
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * Annotated count
     */
    @TableField("annotated_count")
    private Integer annotatedCount;

    /**
     * Annotation status: none-not annotated, partial-partially annotated, completed-completely annotated
     */
    @TableField("annotation_status")
    private String annotationStatus;

    /**
     * Annotation progress percentage
     */
    @TableField("progress")
    private Integer progress;

    /**
     * Annotation rules (JSON format)
     */
    @TableField("annotation_rules")
    private String annotationRules;

    /**
     * Remark
     */
    @TableField("remark")
    private String remark;

    /**
     * Creator
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * Creation time
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

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