package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Recording Schedule Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("recording_schedule")
@ApiModel(value = "RecordingSchedule object", description = "Recording schedule")
public class RecordingSchedule {

    @ApiModelProperty(value = "Primary key ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Device ID")
    @TableField("device_id")
    private Long deviceId;

    @ApiModelProperty(value = "Device name")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty(value = "Schedule name")
    @TableField("schedule_name")
    private String scheduleName;

    @ApiModelProperty(value = "Enabled: 0-Disabled, 1-Enabled")
    @TableField("is_enabled")
    private Boolean isEnabled;

    @ApiModelProperty(value = "Single recording duration (seconds, default 10 minutes)")
    @TableField("record_duration")
    private Integer recordDuration;

    @ApiModelProperty(value = "Recording quality: high-high definition, medium-medium, low-low definition")
    @TableField("record_quality")
    private String recordQuality;

    @ApiModelProperty(value = "Recording format")
    @TableField("record_format")
    private String recordFormat;

    @ApiModelProperty(value = "Storage path")
    @TableField("storage_path")
    private String storagePath;

    @ApiModelProperty(value = "Retention days (0 means permanent retention)")
    @TableField("retention_days")
    private Integer retentionDays;

    @ApiModelProperty(value = "Schedule type: continuous-continuous recording, time_range-time range, time_strategy-time strategy")
    @TableField("schedule_type")
    private String scheduleType;

    @ApiModelProperty(value = "Time strategy ID (when schedule_type is time_strategy)")
    @TableField("time_strategy_id")
    private Long timeStrategyId;

    @ApiModelProperty(value = "Start time (when schedule_type is time_range)")
    @TableField("start_time")
    private LocalTime startTime;

    @ApiModelProperty(value = "End time (when schedule_type is time_range)")
    @TableField("end_time")
    private LocalTime endTime;

    @ApiModelProperty(value = "Weekdays to record (1-7, comma-separated)")
    @TableField("weekdays")
    private String weekdays;

    @ApiModelProperty(value = "Last recording time")
    @TableField("last_record_time")
    private LocalDateTime lastRecordTime;

    @ApiModelProperty(value = "Next recording time")
    @TableField("next_record_time")
    private LocalDateTime nextRecordTime;

    @ApiModelProperty(value = "Total recording times")
    @TableField("total_records")
    private Integer totalRecords;

    @ApiModelProperty(value = "Failed recording times")
    @TableField("failed_records")
    private Integer failedRecords;

    @ApiModelProperty(value = "Creator")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "Updater")
    @TableField("updated_by")
    private String updatedBy;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "Update time")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "Deleted: 0-Not deleted, 1-Deleted")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    // Schedule type constants
    public static final String TYPE_CONTINUOUS = "continuous";
    public static final String TYPE_TIME_RANGE = "time_range";
    public static final String TYPE_TIME_STRATEGY = "time_strategy";

    // Quality constants
    public static final String QUALITY_HIGH = "high";
    public static final String QUALITY_MEDIUM = "medium";
    public static final String QUALITY_LOW = "low";

    /**
     * Get schedule type description
     */
    public String getScheduleTypeText() {
        switch (scheduleType) {
            case TYPE_CONTINUOUS:
                return "Continuous recording";
            case TYPE_TIME_RANGE:
                return "Time range recording";
            case TYPE_TIME_STRATEGY:
                return "Time strategy recording";
            default:
                return "Unknown type";
        }
    }

    /**
     * Get quality description
     */
    public String getRecordQualityText() {
        switch (recordQuality) {
            case QUALITY_HIGH:
                return "High definition";
            case QUALITY_MEDIUM:
                return "Medium";
            case QUALITY_LOW:
                return "Low definition";
            default:
                return "Unknown";
        }
    }

    /**
     * Get formatted recording duration
     */
    public String getFormattedRecordDuration() {
        if (recordDuration == null || recordDuration <= 0) {
            return "0 minutes";
        }
        
        int minutes = recordDuration / 60;
        int seconds = recordDuration % 60;
        
        if (seconds == 0) {
            return minutes + " minutes";
        } else {
            return minutes + "m " + seconds + "s";
        }
    }

    /**
     * Get enabled status description
     */
    public String getEnabledStatusText() {
        return Boolean.TRUE.equals(isEnabled) ? "Enabled" : "Disabled";
    }

    /**
     * Get retention days description
     */
    public String getRetentionDaysText() {
        if (retentionDays == null || retentionDays == 0) {
            return "Permanent retention";
        }
        return retentionDays + " days";
    }

    /**
     * Get success rate
     */
    public Double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        
        int successRecords = totalRecords - (failedRecords == null ? 0 : failedRecords);
        return (double) successRecords / totalRecords * 100;
    }
} 