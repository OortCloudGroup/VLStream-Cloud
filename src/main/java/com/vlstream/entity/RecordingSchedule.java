package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 录制计划实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("recording_schedule")
@ApiModel(value = "RecordingSchedule对象", description = "录制计划")
public class RecordingSchedule {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "设备ID")
    @TableField("device_id")
    private Long deviceId;

    @ApiModelProperty(value = "设备名称")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty(value = "计划名称")
    @TableField("schedule_name")
    private String scheduleName;

    @ApiModelProperty(value = "是否启用：0-禁用，1-启用")
    @TableField("is_enabled")
    private Boolean isEnabled;

    @ApiModelProperty(value = "单次录制时长(秒，默认10分钟)")
    @TableField("record_duration")
    private Integer recordDuration;

    @ApiModelProperty(value = "录制质量：high-高清，medium-中等，low-低清")
    @TableField("record_quality")
    private String recordQuality;

    @ApiModelProperty(value = "录制格式")
    @TableField("record_format")
    private String recordFormat;

    @ApiModelProperty(value = "存储路径")
    @TableField("storage_path")
    private String storagePath;

    @ApiModelProperty(value = "保留天数(0表示永久保留)")
    @TableField("retention_days")
    private Integer retentionDays;

    @ApiModelProperty(value = "计划类型：continuous-连续录制，time_range-时间段，time_strategy-时间策略")
    @TableField("schedule_type")
    private String scheduleType;

    @ApiModelProperty(value = "时间策略ID(当schedule_type为time_strategy时)")
    @TableField("time_strategy_id")
    private Long timeStrategyId;

    @ApiModelProperty(value = "开始时间(当schedule_type为time_range时)")
    @TableField("start_time")
    private LocalTime startTime;

    @ApiModelProperty(value = "结束时间(当schedule_type为time_range时)")
    @TableField("end_time")
    private LocalTime endTime;

    @ApiModelProperty(value = "星期几录制(1-7,逗号分隔)")
    @TableField("weekdays")
    private String weekdays;

    @ApiModelProperty(value = "最后录制时间")
    @TableField("last_record_time")
    private LocalDateTime lastRecordTime;

    @ApiModelProperty(value = "下次录制时间")
    @TableField("next_record_time")
    private LocalDateTime nextRecordTime;

    @ApiModelProperty(value = "总录制次数")
    @TableField("total_records")
    private Integer totalRecords;

    @ApiModelProperty(value = "失败录制次数")
    @TableField("failed_records")
    private Integer failedRecords;

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

    // 计划类型常量
    public static final String TYPE_CONTINUOUS = "continuous";
    public static final String TYPE_TIME_RANGE = "time_range";
    public static final String TYPE_TIME_STRATEGY = "time_strategy";

    // 质量常量
    public static final String QUALITY_HIGH = "high";
    public static final String QUALITY_MEDIUM = "medium";
    public static final String QUALITY_LOW = "low";

    /**
     * 获取计划类型的中文描述
     */
    public String getScheduleTypeText() {
        switch (scheduleType) {
            case TYPE_CONTINUOUS:
                return "连续录制";
            case TYPE_TIME_RANGE:
                return "时间段录制";
            case TYPE_TIME_STRATEGY:
                return "时间策略录制";
            default:
                return "未知类型";
        }
    }

    /**
     * 获取质量的中文描述
     */
    public String getRecordQualityText() {
        switch (recordQuality) {
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

    /**
     * 获取格式化的录制时长
     */
    public String getFormattedRecordDuration() {
        if (recordDuration == null || recordDuration <= 0) {
            return "0分钟";
        }
        
        int minutes = recordDuration / 60;
        int seconds = recordDuration % 60;
        
        if (seconds == 0) {
            return minutes + "分钟";
        } else {
            return minutes + "分" + seconds + "秒";
        }
    }

    /**
     * 获取启用状态的中文描述
     */
    public String getEnabledStatusText() {
        return Boolean.TRUE.equals(isEnabled) ? "启用" : "禁用";
    }

    /**
     * 获取保留天数的描述
     */
    public String getRetentionDaysText() {
        if (retentionDays == null || retentionDays == 0) {
            return "永久保留";
        }
        return retentionDays + "天";
    }

    /**
     * 获取成功率
     */
    public Double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        
        int successRecords = totalRecords - (failedRecords == null ? 0 : failedRecords);
        return (double) successRecords / totalRecords * 100;
    }
} 