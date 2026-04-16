package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Video Recording Record Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("video_record")
@ApiModel(value = "VideoRecord object", description = "Video recording record")
public class VideoRecord {

    @ApiModelProperty(value = "Primary key ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Device ID")
    @TableField("device_id")
    private Long deviceId;

    @ApiModelProperty(value = "Device name")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty(value = "Video file name")
    @TableField("file_name")
    private String fileName;

    @ApiModelProperty(value = "Video file path")
    @TableField("file_path")
    private String filePath;

    @ApiModelProperty(value = "File size (bytes)")
    @TableField("file_size")
    private Long fileSize;

    @ApiModelProperty(value = "Video duration (seconds)")
    @TableField("duration")
    private Integer duration;

    @ApiModelProperty(value = "Video resolution (e.g., 1920x1080)")
    @TableField("resolution")
    private String resolution;

    @ApiModelProperty(value = "Video format")
    @TableField("format")
    private String format;

    @ApiModelProperty(value = "Recording start time")
    @TableField("record_start_time")
    private LocalDateTime recordStartTime;

    @ApiModelProperty(value = "Recording end time")
    @TableField("record_end_time")
    private LocalDateTime recordEndTime;

    @ApiModelProperty(value = "Recording date (for grouping by date)")
    @TableField("record_date")
    private LocalDate recordDate;

    @ApiModelProperty(value = "Recording status: recording-recording, completed-completed, failed-failed, deleted-deleted")
    @TableField("record_status")
    private String recordStatus;

    @ApiModelProperty(value = "Thumbnail path")
    @TableField("thumbnail_path")
    private String thumbnailPath;

    @ApiModelProperty(value = "Recording quality: high-high definition, medium-medium, low-low definition")
    @TableField("quality")
    private String quality;

    @ApiModelProperty(value = "Frame rate")
    @TableField("frame_rate")
    private Integer frameRate;

    @ApiModelProperty(value = "Bitrate (kbps)")
    @TableField("bitrate")
    private Integer bitrate;

    @ApiModelProperty(value = "Error message (when recording fails)")
    @TableField("error_message")
    private String errorMessage;

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

    // Recording status constants
    public static final String STATUS_RECORDING = "recording";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_DELETED = "deleted";

    // Quality constants
    public static final String QUALITY_HIGH = "high";
    public static final String QUALITY_MEDIUM = "medium";
    public static final String QUALITY_LOW = "low";

    // Format constants
    public static final String FORMAT_MP4 = "mp4";
    public static final String FORMAT_AVI = "avi";
    public static final String FORMAT_FLV = "flv";

    /**
     * Get formatted file size
     */
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * Get formatted duration
     */
    public String getFormattedDuration() {
        if (duration == null || duration <= 0) {
            return "00:00:00";
        }
        
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Get recording status description
     */
    public String getRecordStatusText() {
        switch (recordStatus) {
            case STATUS_RECORDING:
                return "Recording";
            case STATUS_COMPLETED:
                return "Completed";
            case STATUS_FAILED:
                return "Recording failed";
            case STATUS_DELETED:
                return "Deleted";
            default:
                return "Unknown status";
        }
    }

    /**
     * Get quality description
     */
    public String getQualityText() {
        switch (quality) {
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
} 