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
 * 标签管理实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tag_management")
@ApiModel(value = "TagManagement对象", description = "标签管理")
public class TagManagement {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "标签名称")
    @TableField("tag_name")
    private String tagName;

    @ApiModelProperty(value = "标签大类(own-自有, public-公共)")
    @TableField("category_type")
    private String categoryType;

    @ApiModelProperty(value = "父级标签ID")
    @TableField("parent_id")
    private Long parentId;

    @ApiModelProperty(value = "层级(0-类型级, 1-父级标签, 2-子级标签)")
    @TableField("level")
    private Integer level;

    @ApiModelProperty(value = "排序序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @ApiModelProperty(value = "标签颜色(十六进制)")
    @TableField("tag_color")
    private String tagColor;

    @ApiModelProperty(value = "标签图标")
    @TableField("tag_icon")
    private String tagIcon;

    @ApiModelProperty(value = "标签描述")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "是否启用(0-禁用, 1-启用)")
    @TableField("is_active")
    private Integer isActive;

    @ApiModelProperty(value = "使用次数")
    @TableField("usage_count")
    private Integer usageCount;

    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "更新人")
    @TableField("updated_by")
    private String updatedBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "是否删除(0-未删除, 1-已删除)")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "子标签列表")
    @TableField(exist = false)
    private List<TagManagement> children;

    @ApiModelProperty(value = "父标签名称")
    @TableField(exist = false)
    private String parentName;
} 