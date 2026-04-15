package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.RecordingSchedule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 录制计划服务接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface RecordingScheduleService extends IService<RecordingSchedule> {

    /**
     * 根据设备ID查询启用的录制计划
     */
    List<RecordingSchedule> getEnabledSchedulesByDevice(Long deviceId);

    /**
     * 查询所有启用的录制计划
     */
    List<RecordingSchedule> getAllEnabledSchedules();

    /**
     * 查询需要执行的录制计划
     */
    List<RecordingSchedule> getSchedulesToExecute();

    /**
     * 根据时间策略ID查询录制计划
     */
    List<RecordingSchedule> getSchedulesByTimeStrategy(Long timeStrategyId);

    /**
     * 创建设备的默认录制计划
     */
    RecordingSchedule createDefaultScheduleForDevice(Long deviceId, String deviceName);

    /**
     * 启用录制计划
     */
    boolean enableSchedule(Long scheduleId, String updatedBy);

    /**
     * 禁用录制计划
     */
    boolean disableSchedule(Long scheduleId, String updatedBy);

    /**
     * 批量启用/禁用录制计划
     */
    boolean batchUpdateEnabledStatus(List<Long> scheduleIds, Boolean enabled, String updatedBy);

    /**
     * 更新录制计划的执行时间
     */
    boolean updateExecutionTime(Long scheduleId, LocalDateTime lastRecordTime, LocalDateTime nextRecordTime);

    /**
     * 增加失败录制次数
     */
    boolean incrementFailedRecords(Long scheduleId);

    /**
     * 计算下次录制时间
     */
    LocalDateTime calculateNextRecordTime(RecordingSchedule schedule);

    /**
     * 检查录制计划是否应该在指定时间执行
     */
    boolean shouldExecuteAt(RecordingSchedule schedule, LocalDateTime currentTime);

    /**
     * 执行录制计划
     */
    boolean executeSchedule(RecordingSchedule schedule);
} 