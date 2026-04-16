package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Annotation Label Entity Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("annotation_label")
public class AnnotationLabel {

    /**
     * Label ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated annotation project ID
     */
    @TableField("annotation_id")
    private Long annotationId;

    /**
     * Label name
     */
    @TableField("name")
    private String name;

    /**
     * Label color (hexadecimal)
     */
    @TableField("color")
    private String color;

    /**
     * Label description
     */
    @TableField("description")
    private String description;

    /**
     * Sort order
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * Usage count
     */
    @TableField("usage_count")
    private Integer usageCount;

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