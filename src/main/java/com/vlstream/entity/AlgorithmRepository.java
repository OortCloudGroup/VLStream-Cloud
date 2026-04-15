package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 算法仓库实体类
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_repository")
public class AlgorithmRepository {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 算法仓库名称
     */
    @TableField("name")
    private String name;

    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;

    /**
     * 拥有算法数量
     */
    @TableField("algorithm_count")
    private Integer algorithmCount;

    /**
     * 仓库类型：basic-基础预置, extended-扩展
     */
    @TableField("repository_type")
    private String repositoryType;

    /**
     * 状态：enabled-启用, disabled-禁用
     */
    @TableField("status")
    private String status;

    /**
     * 创建人
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新人
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
} 