package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.TimeStrategy;
import com.vlstream.mapper.TimeStrategyMapper;
import com.vlstream.service.TimeStrategyService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Time Strategy Service Implementation
 */
@Service
public class TimeStrategyServiceImpl extends ServiceImpl<TimeStrategyMapper, TimeStrategy> implements TimeStrategyService {
    
    @Override
    public TimeStrategy getByDeviceId(String deviceId) {
        QueryWrapper<TimeStrategy> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean saveOrUpdateStrategy(TimeStrategy timeStrategy) {
        // Check if time strategy already exists for this device
        TimeStrategy existing = getByDeviceId(timeStrategy.getDeviceId());
        
        if (existing != null) {
            // If exists, update
            UpdateWrapper<TimeStrategy> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("device_id", timeStrategy.getDeviceId());
            timeStrategy.setId(existing.getId());
            return this.update(timeStrategy, updateWrapper);
        } else {
            // If not exists, add
            return this.save(timeStrategy);
        }
    }
    
    @Override
    public boolean deleteByDeviceId(String deviceId) {
        QueryWrapper<TimeStrategy> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId);
        return this.remove(wrapper);
    }
    
    @Override
    public List<TimeStrategy> getTimeStrategiesToExecute() {
        List<TimeStrategy> strategiesToExecute = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        // 获取所有时间策略
        List<TimeStrategy> allStrategies = this.list();
        
        for (TimeStrategy strategy : allStrategies) {
            if (shouldExecuteStrategy(strategy, currentTime, currentDay)) {
                strategiesToExecute.add(strategy);
            }
        }
        
        return strategiesToExecute;
    }
    
    /**
     * Determine if the time strategy should be executed
     */
    private boolean shouldExecuteStrategy(TimeStrategy strategy, LocalTime currentTime, DayOfWeek currentDay) {
        if (strategy.getStrategyType().equals("weekly")) {
            // 每周策略
            Map<String, List<Integer>> weeklyTimes = strategy.getWeeklyTimes();
            if (weeklyTimes != null) {
                String dayKey = getDayKey(currentDay);
                List<Integer> hours = weeklyTimes.get(dayKey);
                if (hours != null && !hours.isEmpty()) {
                    // 检查当前时间是否在配置的小时范围内
                    int currentHour = currentTime.getHour();
                    return hours.contains(currentHour);
                }
            }
        } else if (strategy.getStrategyType().equals("daily")) {
            // 每天策略
            List<Integer> dailyTimes = strategy.getDailyTimes();
            if (dailyTimes != null && !dailyTimes.isEmpty()) {
                int currentHour = currentTime.getHour();
                return dailyTimes.contains(currentHour);
            }
        }
        
        return false;
    }
    
    /**
     * Get the day of week key name
     */
    private String getDayKey(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "monday";
            case TUESDAY: return "tuesday";
            case WEDNESDAY: return "wednesday";
            case THURSDAY: return "thursday";
            case FRIDAY: return "friday";
            case SATURDAY: return "saturday";
            case SUNDAY: return "sunday";
            default: return "monday";
        }
    }
} 