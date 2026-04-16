package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.RecordingSchedule;
import com.vlstream.mapper.RecordingScheduleMapper;
import com.vlstream.service.RecordingScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recording Schedule Service Implementation Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RecordingScheduleServiceImpl extends ServiceImpl<RecordingScheduleMapper, RecordingSchedule> implements RecordingScheduleService {

    @Autowired
    private RecordingScheduleMapper recordingScheduleMapper;

    @Override
    public List<RecordingSchedule> getEnabledSchedulesByDevice(Long deviceId) {
        LambdaQueryWrapper<RecordingSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecordingSchedule::getDeviceId, deviceId)
               .eq(RecordingSchedule::getIsEnabled, true)
               .eq(RecordingSchedule::getDeleted, 0);
        return list(wrapper);
    }

    @Override
    public List<RecordingSchedule> getAllEnabledSchedules() {
        LambdaQueryWrapper<RecordingSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecordingSchedule::getIsEnabled, true)
               .eq(RecordingSchedule::getDeleted, 0);
        return list(wrapper);
    }

    @Override
    public List<RecordingSchedule> getSchedulesToExecute() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<RecordingSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecordingSchedule::getIsEnabled, true)
               .eq(RecordingSchedule::getDeleted, 0)
               .le(RecordingSchedule::getNextRecordTime, now);
        return list(wrapper);
    }

    @Override
    public List<RecordingSchedule> getSchedulesByTimeStrategy(Long timeStrategyId) {
        LambdaQueryWrapper<RecordingSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecordingSchedule::getTimeStrategyId, timeStrategyId)
               .eq(RecordingSchedule::getDeleted, 0);
        return list(wrapper);
    }

    @Override
    public RecordingSchedule createDefaultScheduleForDevice(Long deviceId, String deviceName) {
        try {
            RecordingSchedule schedule = new RecordingSchedule();
            schedule.setDeviceId(deviceId);
            schedule.setDeviceName(deviceName);
            schedule.setScheduleName("Default Recording Schedule-" + deviceName);
            schedule.setScheduleType(RecordingSchedule.TYPE_CONTINUOUS);
            schedule.setIsEnabled(true);
            schedule.setRecordDuration(600); // 10 minutes
            schedule.setRetentionDays(30); // Retain for 30 days
            schedule.setNextRecordTime(LocalDateTime.now());
            schedule.setCreatedBy("system");
            
            if (save(schedule)) {
                log.info("Default recording schedule created successfully: deviceId={}, scheduleId={}", deviceId, schedule.getId());
                return schedule;
            } else {
                log.error("Failed to create default recording schedule: deviceId={}", deviceId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating default recording schedule: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean enableSchedule(Long scheduleId, String updatedBy) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("Recording schedule does not exist: scheduleId={}", scheduleId);
                return false;
            }
            
            schedule.setIsEnabled(true);
            schedule.setUpdatedBy(updatedBy);
            schedule.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(schedule);
            if (success) {
                log.info("Recording schedule enabled successfully: scheduleId={}", scheduleId);
            } else {
                log.error("Failed to enable recording schedule: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("Error enabling recording schedule: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean disableSchedule(Long scheduleId, String updatedBy) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("Recording schedule does not exist: scheduleId={}", scheduleId);
                return false;
            }
            
            schedule.setIsEnabled(false);
            schedule.setUpdatedBy(updatedBy);
            schedule.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(schedule);
            if (success) {
                log.info("Recording schedule disabled successfully: scheduleId={}", scheduleId);
            } else {
                log.error("Failed to disable recording schedule: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("Error disabling recording schedule: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchUpdateEnabledStatus(List<Long> scheduleIds, Boolean enabled, String updatedBy) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return false;
        }
        
        try {
            List<RecordingSchedule> schedules = listByIds(scheduleIds);
            for (RecordingSchedule schedule : schedules) {
                schedule.setIsEnabled(enabled);
                schedule.setUpdatedBy(updatedBy);
                schedule.setUpdateTime(LocalDateTime.now());
            }
            
            boolean success = updateBatchById(schedules);
            if (success) {
                log.info("Batch update recording schedule status successful: count={}, enabled={}", schedules.size(), enabled);
            } else {
                log.error("Failed to batch update recording schedule status: scheduleIds={}, enabled={}", scheduleIds, enabled);
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch updating recording schedule status: scheduleIds={}, enabled={}, error={}", 
                     scheduleIds, enabled, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateExecutionTime(Long scheduleId, LocalDateTime lastRecordTime, LocalDateTime nextRecordTime) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("Recording schedule does not exist: scheduleId={}", scheduleId);
                return false;
            }
            
            schedule.setLastRecordTime(lastRecordTime);
            schedule.setNextRecordTime(nextRecordTime);
            schedule.setUpdateTime(LocalDateTime.now());
            
            // Increase total recording count
            Integer totalRecords = schedule.getTotalRecords();
            schedule.setTotalRecords(totalRecords != null ? totalRecords + 1 : 1);
            
            boolean success = updateById(schedule);
            if (success) {
                log.debug("Recording schedule execution time updated successfully: scheduleId={}, nextRecordTime={}", 
                         scheduleId, nextRecordTime);
            } else {
                log.error("Failed to update recording schedule execution time: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("Error updating recording schedule execution time: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean incrementFailedRecords(Long scheduleId) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("Recording schedule does not exist: scheduleId={}", scheduleId);
                return false;
            }
            
            Integer failedRecords = schedule.getFailedRecords();
            schedule.setFailedRecords(failedRecords != null ? failedRecords + 1 : 1);
            schedule.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(schedule);
            if (success) {
                log.debug("Recording schedule failed records incremented successfully: scheduleId={}, failedRecords={}", 
                         scheduleId, schedule.getFailedRecords());
            } else {
                log.error("Failed to increment recording schedule failed records: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("Error incrementing recording schedule failed records: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public LocalDateTime calculateNextRecordTime(RecordingSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            String scheduleType = schedule.getScheduleType();
            
            if (RecordingSchedule.TYPE_CONTINUOUS.equals(scheduleType)) {
                // Continuous recording: start next recording immediately
                return now.plusMinutes(1);
            } else if (RecordingSchedule.TYPE_TIME_RANGE.equals(scheduleType)) {
                // Time range recording: calculate based on time configuration
                return now.plusMinutes(10); // Simplified implementation, record again after 10 minutes
            } else if (RecordingSchedule.TYPE_TIME_STRATEGY.equals(scheduleType)) {
                // Time strategy recording: calculate based on time strategy
                return now.plusMinutes(10); // Simplified implementation, record again after 10 minutes
            }
            
            // Default: record again after 10 minutes
            return now.plusMinutes(10);
        } catch (Exception e) {
            log.error("Error calculating next recording time: scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
            return now.plusMinutes(10);
        }
    }

    @Override
    public boolean shouldExecuteAt(RecordingSchedule schedule, LocalDateTime currentTime) {
        if (schedule == null || !schedule.getIsEnabled() || schedule.getDeleted() == 1) {
            return false;
        }
        
        LocalDateTime nextRecordTime = schedule.getNextRecordTime();
        if (nextRecordTime == null) {
            return true; // If next recording time is not set, execute immediately
        }
        
        return currentTime.isAfter(nextRecordTime) || currentTime.isEqual(nextRecordTime);
    }

    @Override
    public boolean executeSchedule(RecordingSchedule schedule) {
        try {
            log.info("Executing recording schedule: scheduleId={}, deviceId={}, scheduleName={}", 
                    schedule.getId(), schedule.getDeviceId(), schedule.getScheduleName());
            
            // Update execution time
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextTime = calculateNextRecordTime(schedule);
            updateExecutionTime(schedule.getId(), now, nextTime);
            
            return true;
        } catch (Exception e) {
            log.error("Error executing recording schedule: scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
            incrementFailedRecords(schedule.getId());
            return false;
        }
    }
} 