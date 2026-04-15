package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Event management entity mapped to table event_management.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("event_management")
@ApiModel(value = "EventManagement", description = "Event management record")
public class EventManagement {

    @ApiModelProperty("Primary key")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("事件描述")
    @TableField("event_desc")
    private String eventDesc;

    @ApiModelProperty("事件类型")
    @TableField("event_type")
    private String eventType;

    @ApiModelProperty("上报位置")
    @TableField("report_location")
    private String reportLocation;

    @ApiModelProperty("上报设备")
    @TableField("report_device")
    private String reportDevice;

    @ApiModelProperty("上报图片")
    @TableField("report_img")
    private String reportImg;

    @ApiModelProperty("上报时间")
    @TableField("report_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;

    @ApiModelProperty("事件级别：low-低,medium-中,high-高,urgent-紧急")
    @TableField("event_level")
    private String eventLevel;

    @ApiModelProperty("事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭")
    @TableField("event_status")
    private String eventStatus;

    @ApiModelProperty("执行人")
    @TableField("executor")
    private String executor;

    @ApiModelProperty("执行人ID列表")
    @TableField("executor_ids")
    private String executorIds;

    @ApiModelProperty("事件数据")
    @TableField("event_data")
    private String eventData;

    @ApiModelProperty("处理结果")
    @TableField("handle_result")
    private String handleResult;

    @ApiModelProperty("反馈信息")
    @TableField("feedback_info")
    private String feedbackInfo;

    @ApiModelProperty("反馈图片")
    @TableField("feedback_img")
    private String feedbackImg;

    @ApiModelProperty("反馈状态")
    @TableField("feedback_status")
    private Integer feedbackStatus;

    @ApiModelProperty("创建人")
    @TableField("created_by")
    private Long createdBy;

    @ApiModelProperty("创建时间")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @ApiModelProperty("更新时间")
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    @ApiModelProperty("逻辑删除: 0-normal,1-deleted")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
