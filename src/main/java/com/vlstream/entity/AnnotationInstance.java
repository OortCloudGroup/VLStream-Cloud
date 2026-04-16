package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Annotation Instance Entity Class (specific annotation data)
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("annotation_instance")
public class AnnotationInstance {

    /**
     * Instance ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated annotation project ID
     */
    @TableField("annotation_id")
    private Long annotationId;

    /**
     * Label ID
     */
    @TableField("label_id")
    private Long labelId;

    /**
     * Image ID
     */
    @TableField("image_id")
    private Long imageId;

    /**
     * Annotation type: rect-rectangle, circle-circle, polygon-polygon
     */
    @TableField("annotation_type")
    private String annotationType;

    /**
     * Annotation coordinate data (JSON format)
     */
    @TableField("annotation_data")
    private String annotationData;

    /**
     * Confidence
     */
    @TableField("confidence")
    private BigDecimal confidence;

    /**
     * Verified
     */
    @TableField("verified")
    private Boolean verified;

    /**
     * Creator ID
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