package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.RecordingSchedule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recording schedule service interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface RecordingScheduleService extends IService<RecordingSchedule> {

    /**
     * Query enabled recording schedules by device ID
     */
    List<RecordingSchedule> getEnabledSchedulesByDevice(Long deviceId);

    /**
     * Query all enabled recording schedules
     */
    List<RecordingSchedule> getAllEnabledSchedules();

    /**
     * Query recording schedules that need to be executed
     */
    List<RecordingSchedule> getSchedulesToExecute();

    /**
     * Query recording schedules by time strategy ID
     */
    List<RecordingSchedule> getSchedulesByTimeStrategy(Long timeStrategyId);

    /**
     * Create default recording schedule for device
     */
    RecordingSchedule createDefaultScheduleForDevice(Long deviceId, String deviceName);

    /**
     * Enable recording schedule
     */
    boolean enableSchedule(Long scheduleId, String updatedBy);

    /**
     * Disable recording schedule
     */
    boolean disableSchedule(Long scheduleId, String updatedBy);

    /**
     * Batch enable/disable recording schedules
     */
    boolean batchUpdateEnabledStatus(List<Long> scheduleIds, Boolean enabled, String updatedBy);

    /**
     * Update execution time of recording schedule
     */
    boolean updateExecutionTime(Long scheduleId, LocalDateTime lastRecordTime, LocalDateTime nextRecordTime);

    /**
     * Increment failed recording count
     */
    boolean incrementFailedRecords(Long scheduleId);

    /**
     * Calculate next recording time
     */
    LocalDateTime calculateNextRecordTime(RecordingSchedule schedule);

    /**
     * Check if recording schedule should be executed at specified time
     */
    boolean shouldExecuteAt(RecordingSchedule schedule, LocalDateTime currentTime);

    /**
     * Execute recording schedule
     */
    boolean executeSchedule(RecordingSchedule schedule);
} 