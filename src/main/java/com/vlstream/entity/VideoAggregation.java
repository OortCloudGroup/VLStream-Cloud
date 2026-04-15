package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 视频汇聚配置实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vls_video_aggregation")
@ApiModel(value = "VideoAggregation对象", description = "视频汇聚配置")
public class VideoAggregation {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "汇聚名称")
    @TableField("aggregation_name")
    private String aggregationName;

    @ApiModelProperty(value = "汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)")
    @TableField("aggregation_type")
    private Integer aggregationType;

    @ApiModelProperty(value = "画面布局(1x1,2x2,3x3,4x4,自定义)")
    @TableField("layout")
    private String layout;

    @ApiModelProperty(value = "输出分辨率")
    @TableField("output_resolution")
    private String outputResolution;

    @ApiModelProperty(value = "输出帧率")
    @TableField("output_frame_rate")
    private Integer outputFrameRate;

    @ApiModelProperty(value = "输出比特率")
    @TableField("output_bit_rate")
    private Integer outputBitRate;

    @ApiModelProperty(value = "源流ID列表(JSON格式)")
    @TableField("source_stream_ids")
    private String sourceStreamIds;

    @ApiModelProperty(value = "输出流地址")
    @TableField("output_stream_url")
    private String outputStreamUrl;

    @ApiModelProperty(value = "状态(0-停止,1-运行,2-异常)")
    @TableField("status")
    private Integer status;

    @ApiModelProperty(value = "切换策略(1-手动,2-自动,3-定时)")
    @TableField("switch_strategy")
    private Integer switchStrategy;

    @ApiModelProperty(value = "切换间隔(秒)")
    @TableField("switch_interval")
    private Integer switchInterval;

    @ApiModelProperty(value = "描述")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "是否启用(0-禁用,1-启用)")
    @TableField("enabled")
    private Integer enabled;

    @ApiModelProperty(value = "是否删除(0-未删除,1-已删除)")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("creator")
    private String creator;

    @ApiModelProperty(value = "更新人")
    @TableField("updater")
    private String updater;
} 