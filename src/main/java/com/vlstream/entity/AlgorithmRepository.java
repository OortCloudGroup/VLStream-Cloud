package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Algorithm Repository Entity Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_repository")
public class AlgorithmRepository {

    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Algorithm repository name
     */
    @TableField("name")
    private String name;

    /**
     * Remark
     */
    @TableField("remark")
    private String remark;

    /**
     * Number of algorithms
     */
    @TableField("algorithm_count")
    private Integer algorithmCount;

    /**
     * Repository type: basic-built-in, extended-extended
     */
    @TableField("repository_type")
    private String repositoryType;

    /**
     * Status: enabled-enabled, disabled-disabled
     */
    @TableField("status")
    private String status;

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