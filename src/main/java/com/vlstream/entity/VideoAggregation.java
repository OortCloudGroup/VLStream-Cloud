package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * Video Aggregation Configuration Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vls_video_aggregation")
@ApiModel(value = "VideoAggregation object", description = "Video aggregation configuration")
public class VideoAggregation {

    @ApiModelProperty(value = "Primary key ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Aggregation name")
    @TableField("aggregation_name")
    private String aggregationName;

    @ApiModelProperty(value = "Aggregation type (1-Split screen, 2-Picture-in-picture, 3-Rotation, 4-Intelligent switching)")
    @TableField("aggregation_type")
    private Integer aggregationType;

    @ApiModelProperty(value = "Screen layout (1x1, 2x2, 3x3, 4x4, custom)")
    @TableField("layout")
    private String layout;

    @ApiModelProperty(value = "Output resolution")
    @TableField("output_resolution")
    private String outputResolution;

    @ApiModelProperty(value = "Output frame rate")
    @TableField("output_frame_rate")
    private Integer outputFrameRate;

    @ApiModelProperty(value = "Output bit rate")
    @TableField("output_bit_rate")
    private Integer outputBitRate;

    @ApiModelProperty(value = "Source stream ID list (JSON format)")
    @TableField("source_stream_ids")
    private String sourceStreamIds;

    @ApiModelProperty(value = "Output stream URL")
    @TableField("output_stream_url")
    private String outputStreamUrl;

    @ApiModelProperty(value = "Status (0-Stop, 1-Running, 2-Exception)")
    @TableField("status")
    private Integer status;

    @ApiModelProperty(value = "Switching strategy (1-Manual, 2-Automatic, 3-Timed)")
    @TableField("switch_strategy")
    private Integer switchStrategy;

    @ApiModelProperty(value = "Switching interval (seconds)")
    @TableField("switch_interval")
    private Integer switchInterval;

    @ApiModelProperty(value = "Description")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "Enabled (0-Disabled, 1-Enabled)")
    @TableField("enabled")
    private Integer enabled;

    @ApiModelProperty(value = "Deleted (0-Not deleted, 1-Deleted)")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "Update time")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "Creator")
    @TableField("creator")
    private String creator;

    @ApiModelProperty(value = "Updater")
    @TableField("updater")
    private String updater;
} 