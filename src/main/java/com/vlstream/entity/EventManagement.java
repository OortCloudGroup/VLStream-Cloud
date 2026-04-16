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

    @ApiModelProperty("Event description")
    @TableField("event_desc")
    private String eventDesc;

    @ApiModelProperty("Event type")
    @TableField("event_type")
    private String eventType;

    @ApiModelProperty("Report location")
    @TableField("report_location")
    private String reportLocation;

    @ApiModelProperty("Report device")
    @TableField("report_device")
    private String reportDevice;

    @ApiModelProperty("Report image")
    @TableField("report_img")
    private String reportImg;

    @ApiModelProperty("Report time")
    @TableField("report_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;

    @ApiModelProperty("Event level: low-low, medium-medium, high-high, urgent-urgent")
    @TableField("event_level")
    private String eventLevel;

    @ApiModelProperty("Event status: pending-pending, processing-processing, completed-completed, closed-closed")
    @TableField("event_status")
    private String eventStatus;

    @ApiModelProperty("Executor")
    @TableField("executor")
    private String executor;

    @ApiModelProperty("Executor ID list")
    @TableField("executor_ids")
    private String executorIds;

    @ApiModelProperty("Event data")
    @TableField("event_data")
    private String eventData;

    @ApiModelProperty("Handling result")
    @TableField("handle_result")
    private String handleResult;

    @ApiModelProperty("Feedback information")
    @TableField("feedback_info")
    private String feedbackInfo;

    @ApiModelProperty("Feedback image")
    @TableField("feedback_img")
    private String feedbackImg;

    @ApiModelProperty("Feedback status")
    @TableField("feedback_status")
    private Integer feedbackStatus;

    @ApiModelProperty("Creator")
    @TableField("created_by")
    private Long createdBy;

    @ApiModelProperty("Creation time")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @ApiModelProperty("Update time")
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    @ApiModelProperty("Logical deletion: 0-normal, 1-deleted")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
