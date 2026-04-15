package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 设备标签关联实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("device_tag_relation")
@ApiModel(value = "DeviceTagRelation对象", description = "设备标签关联")
public class DeviceTagRelation {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "设备ID")
    @TableField("device_id")
    private Long deviceId;

    @ApiModelProperty(value = "标签ID")
    @TableField("tag_id")
    private Long tagId;

    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    // 以下为关联对象，不对应数据库字段
    @ApiModelProperty(value = "设备信息", hidden = true)
    @TableField(exist = false)
    private DeviceInfo deviceInfo;

    @ApiModelProperty(value = "标签信息", hidden = true)
    @TableField(exist = false)
    private TagManagement tagInfo;

    @ApiModelProperty(value = "标签名称", hidden = true)
    @TableField(exist = false)
    private String tagName;

    @ApiModelProperty(value = "标签类型", hidden = true)
    @TableField(exist = false)
    private String categoryType;

    @ApiModelProperty(value = "标签颜色", hidden = true)
    @TableField(exist = false)
    private String tagColor;
} 