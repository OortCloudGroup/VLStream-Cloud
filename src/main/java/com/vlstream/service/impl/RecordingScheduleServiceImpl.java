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
 * 录制计划服务实现类
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
            schedule.setScheduleName("默认录制计划-" + deviceName);
            schedule.setScheduleType(RecordingSchedule.TYPE_CONTINUOUS);
            schedule.setIsEnabled(true);
            schedule.setRecordDuration(600); // 10分钟
            schedule.setRetentionDays(30); // 保留30天
            schedule.setNextRecordTime(LocalDateTime.now());
            schedule.setCreatedBy("system");
            
            if (save(schedule)) {
                log.info("创建默认录制计划成功: deviceId={}, scheduleId={}", deviceId, schedule.getId());
                return schedule;
            } else {
                log.error("创建默认录制计划失败: deviceId={}", deviceId);
                return null;
            }
        } catch (Exception e) {
            log.error("创建默认录制计划异常: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean enableSchedule(Long scheduleId, String updatedBy) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("录制计划不存在: scheduleId={}", scheduleId);
                return false;
            }
            
            schedule.setIsEnabled(true);
            schedule.setUpdatedBy(updatedBy);
            schedule.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(schedule);
            if (success) {
                log.info("启用录制计划成功: scheduleId={}", scheduleId);
            } else {
                log.error("启用录制计划失败: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("启用录制计划异常: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean disableSchedule(Long scheduleId, String updatedBy) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("录制计划不存在: scheduleId={}", scheduleId);
                return false;
            }
            
            schedule.setIsEnabled(false);
            schedule.setUpdatedBy(updatedBy);
            schedule.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(schedule);
            if (success) {
                log.info("禁用录制计划成功: scheduleId={}", scheduleId);
            } else {
                log.error("禁用录制计划失败: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("禁用录制计划异常: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
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
                log.info("批量更新录制计划状态成功: 数量={}, enabled={}", schedules.size(), enabled);
            } else {
                log.error("批量更新录制计划状态失败: scheduleIds={}, enabled={}", scheduleIds, enabled);
            }
            return success;
        } catch (Exception e) {
            log.error("批量更新录制计划状态异常: scheduleIds={}, enabled={}, error={}", 
                     scheduleIds, enabled, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateExecutionTime(Long scheduleId, LocalDateTime lastRecordTime, LocalDateTime nextRecordTime) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("录制计划不存在: scheduleId={}", scheduleId);
                return false;
            }
            
            schedule.setLastRecordTime(lastRecordTime);
            schedule.setNextRecordTime(nextRecordTime);
            schedule.setUpdateTime(LocalDateTime.now());
            
            // 增加总录制次数
            Integer totalRecords = schedule.getTotalRecords();
            schedule.setTotalRecords(totalRecords != null ? totalRecords + 1 : 1);
            
            boolean success = updateById(schedule);
            if (success) {
                log.debug("更新录制计划执行时间成功: scheduleId={}, nextRecordTime={}", 
                         scheduleId, nextRecordTime);
            } else {
                log.error("更新录制计划执行时间失败: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("更新录制计划执行时间异常: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean incrementFailedRecords(Long scheduleId) {
        try {
            RecordingSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("录制计划不存在: scheduleId={}", scheduleId);
                return false;
            }
            
            Integer failedRecords = schedule.getFailedRecords();
            schedule.setFailedRecords(failedRecords != null ? failedRecords + 1 : 1);
            schedule.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(schedule);
            if (success) {
                log.debug("增加录制计划失败次数成功: scheduleId={}, failedRecords={}", 
                         scheduleId, schedule.getFailedRecords());
            } else {
                log.error("增加录制计划失败次数失败: scheduleId={}", scheduleId);
            }
            return success;
        } catch (Exception e) {
            log.error("增加录制计划失败次数异常: scheduleId={}, error={}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public LocalDateTime calculateNextRecordTime(RecordingSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            String scheduleType = schedule.getScheduleType();
            
            if (RecordingSchedule.TYPE_CONTINUOUS.equals(scheduleType)) {
                // 连续录制：立即开始下一次录制
                return now.plusMinutes(1);
            } else if (RecordingSchedule.TYPE_TIME_RANGE.equals(scheduleType)) {
                // 时间段录制：根据时间配置计算
                return now.plusMinutes(10); // 简化实现，10分钟后再次录制
            } else if (RecordingSchedule.TYPE_TIME_STRATEGY.equals(scheduleType)) {
                // 时间策略录制：根据时间策略计算
                return now.plusMinutes(10); // 简化实现，10分钟后再次录制
            }
            
            // 默认10分钟后再次录制
            return now.plusMinutes(10);
        } catch (Exception e) {
            log.error("计算下次录制时间异常: scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
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
            return true; // 如果没有设置下次录制时间，则立即执行
        }
        
        return currentTime.isAfter(nextRecordTime) || currentTime.isEqual(nextRecordTime);
    }

    @Override
    public boolean executeSchedule(RecordingSchedule schedule) {
        try {
            log.info("执行录制计划: scheduleId={}, deviceId={}, scheduleName={}", 
                    schedule.getId(), schedule.getDeviceId(), schedule.getScheduleName());
            
            // 更新执行时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextTime = calculateNextRecordTime(schedule);
            updateExecutionTime(schedule.getId(), now, nextTime);
            
            return true;
        } catch (Exception e) {
            log.error("执行录制计划异常: scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
            incrementFailedRecords(schedule.getId());
            return false;
        }
    }
} 