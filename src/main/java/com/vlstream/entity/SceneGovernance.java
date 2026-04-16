package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * Scene Governance Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("scene_governance")
@ApiModel(value = "SceneGovernance object", description = "Scene governance")
public class SceneGovernance {

    @ApiModelProperty(value = "Primary key ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Scene name")
    @TableField("name")
    private String name;

    @ApiModelProperty(value = "Scene description/remark")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "Associated devices")
    @TableField("devices")
    private String devices;

    @ApiModelProperty(value = "Governance rules")
    @TableField("rules")
    private String rules;

    @ApiModelProperty(value = "Status: enabled-enabled, disabled-disabled")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "Execution type: daily-daily, alternate-every other day, weekly-weekly, monthly-monthly")
    @TableField("execute_type")
    private String executeType;

    @ApiModelProperty(value = "Selected days (JSON array)")
    @TableField("selected_days")
    private String selectedDays;

    @ApiModelProperty(value = "Interval number")
    @TableField("interval_num")
    private Integer intervalNum;

    @ApiModelProperty(value = "AI algorithm")
    @TableField("algorithm")
    private String algorithm;

    @ApiModelProperty(value = "Regional location")
    @TableField("location")
    private String location;

    @ApiModelProperty(value = "Cameras")
    @TableField("cameras")
    private String cameras;

    @ApiModelProperty(value = "Start time")
    @TableField("start_time")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "End time")
    @TableField("end_time")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "Update time")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "Deleted: 0-Not deleted, 1-Deleted")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "Creator")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "Updater")
    @TableField("updated_by")
    private String updatedBy;
} 