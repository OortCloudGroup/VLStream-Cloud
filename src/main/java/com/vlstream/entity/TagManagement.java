package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tag Management Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tag_management")
@ApiModel(value = "TagManagement object", description = "Tag management")
public class TagManagement {

    @ApiModelProperty(value = "Primary key ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Tag name")
    @TableField("tag_name")
    private String tagName;

    @ApiModelProperty(value = "Tag category (own-own, public-public)")
    @TableField("category_type")
    private String categoryType;

    @ApiModelProperty(value = "Parent tag ID")
    @TableField("parent_id")
    private Long parentId;

    @ApiModelProperty(value = "Level (0-type level, 1-parent tag, 2-child tag)")
    @TableField("level")
    private Integer level;

    @ApiModelProperty(value = "Sort order")
    @TableField("sort_order")
    private Integer sortOrder;

    @ApiModelProperty(value = "Tag color (hexadecimal)")
    @TableField("tag_color")
    private String tagColor;

    @ApiModelProperty(value = "Tag icon")
    @TableField("tag_icon")
    private String tagIcon;

    @ApiModelProperty(value = "Tag description")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "Active status (0-Disabled, 1-Enabled)")
    @TableField("is_active")
    private Integer isActive;

    @ApiModelProperty(value = "Usage count")
    @TableField("usage_count")
    private Integer usageCount;

    @ApiModelProperty(value = "Creator")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "Updater")
    @TableField("updated_by")
    private String updatedBy;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "Update time")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "Deleted (0-Not deleted, 1-Deleted)")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "Child tag list")
    @TableField(exist = false)
    private List<TagManagement> children;

    @ApiModelProperty(value = "Parent tag name")
    @TableField(exist = false)
    private String parentName;
} 