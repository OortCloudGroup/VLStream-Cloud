package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 视频录制记录实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("video_record")
@ApiModel(value = "VideoRecord对象", description = "视频录制记录")
public class VideoRecord {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "设备ID")
    @TableField("device_id")
    private Long deviceId;

    @ApiModelProperty(value = "设备名称")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty(value = "视频文件名")
    @TableField("file_name")
    private String fileName;

    @ApiModelProperty(value = "视频文件路径")
    @TableField("file_path")
    private String filePath;

    @ApiModelProperty(value = "文件大小(字节)")
    @TableField("file_size")
    private Long fileSize;

    @ApiModelProperty(value = "视频时长(秒)")
    @TableField("duration")
    private Integer duration;

    @ApiModelProperty(value = "视频分辨率(如1920x1080)")
    @TableField("resolution")
    private String resolution;

    @ApiModelProperty(value = "视频格式")
    @TableField("format")
    private String format;

    @ApiModelProperty(value = "录制开始时间")
    @TableField("record_start_time")
    private LocalDateTime recordStartTime;

    @ApiModelProperty(value = "录制结束时间")
    @TableField("record_end_time")
    private LocalDateTime recordEndTime;

    @ApiModelProperty(value = "录制日期(用于按日期分组)")
    @TableField("record_date")
    private LocalDate recordDate;

    @ApiModelProperty(value = "录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除")
    @TableField("record_status")
    private String recordStatus;

    @ApiModelProperty(value = "缩略图路径")
    @TableField("thumbnail_path")
    private String thumbnailPath;

    @ApiModelProperty(value = "录制质量：high-高清，medium-中等，low-低清")
    @TableField("quality")
    private String quality;

    @ApiModelProperty(value = "帧率")
    @TableField("frame_rate")
    private Integer frameRate;

    @ApiModelProperty(value = "比特率(kbps)")
    @TableField("bitrate")
    private Integer bitrate;

    @ApiModelProperty(value = "错误信息(录制失败时)")
    @TableField("error_message")
    private String errorMessage;

    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "更新人")
    @TableField("updated_by")
    private String updatedBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "是否删除：0-未删除，1-已删除")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    // 录制状态常量
    public static final String STATUS_RECORDING = "recording";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_DELETED = "deleted";

    // 质量常量
    public static final String QUALITY_HIGH = "high";
    public static final String QUALITY_MEDIUM = "medium";
    public static final String QUALITY_LOW = "low";

    // 格式常量
    public static final String FORMAT_MP4 = "mp4";
    public static final String FORMAT_AVI = "avi";
    public static final String FORMAT_FLV = "flv";

    /**
     * 获取格式化的文件大小
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
     * 获取格式化的时长
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
     * 获取录制状态的中文描述
     */
    public String getRecordStatusText() {
        switch (recordStatus) {
            case STATUS_RECORDING:
                return "录制中";
            case STATUS_COMPLETED:
                return "已完成";
            case STATUS_FAILED:
                return "录制失败";
            case STATUS_DELETED:
                return "已删除";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取质量的中文描述
     */
    public String getQualityText() {
        switch (quality) {
            case QUALITY_HIGH:
                return "高清";
            case QUALITY_MEDIUM:
                return "中等";
            case QUALITY_LOW:
                return "低清";
            default:
                return "未知";
        }
    }
} 