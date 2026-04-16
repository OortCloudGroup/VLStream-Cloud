package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Device Tag Relation Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("device_tag_relation")
@ApiModel(value = "DeviceTagRelation object", description = "Device tag relation")
public class DeviceTagRelation {

    @ApiModelProperty(value = "Primary key ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Device ID")
    @TableField("device_id")
    private Long deviceId;

    @ApiModelProperty(value = "Tag ID")
    @TableField("tag_id")
    private Long tagId;

    @ApiModelProperty(value = "Creator")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    // The following are associated objects, not corresponding to database fields
    @ApiModelProperty(value = "Device information", hidden = true)
    @TableField(exist = false)
    private DeviceInfo deviceInfo;

    @ApiModelProperty(value = "Tag information", hidden = true)
    @TableField(exist = false)
    private TagManagement tagInfo;

    @ApiModelProperty(value = "Tag name", hidden = true)
    @TableField(exist = false)
    private String tagName;

    @ApiModelProperty(value = "Tag type", hidden = true)
    @TableField(exist = false)
    private String categoryType;

    @ApiModelProperty(value = "Tag color", hidden = true)
    @TableField(exist = false)
    private String tagColor;
} 