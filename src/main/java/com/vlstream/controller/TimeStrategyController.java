package com.vlstream.controller;

import com.vlstream.common.Result;
import com.vlstream.entity.TimeStrategy;
import com.vlstream.service.TimeStrategyService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 时间策略控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/time-strategy")
@RequiredArgsConstructor
@Api(tags = "时间策略管理")
public class TimeStrategyController {
    
    private final TimeStrategyService timeStrategyService;
    
    /**
     * 根据设备ID获取时间策略
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "获取时间策略", description = "根据设备ID获取时间策略")
    public Result<TimeStrategy> getByDeviceId(@PathVariable String deviceId) {
        log.info("获取设备时间策略，设备ID: {}", deviceId);
        TimeStrategy timeStrategy = timeStrategyService.getByDeviceId(deviceId);
        
        // 添加调试日志
        if (timeStrategy != null) {
            log.info("查询到时间策略 - ID: {}, 设备ID: {}, 策略类型: {}", 
                timeStrategy.getId(), timeStrategy.getDeviceId(), timeStrategy.getStrategyType());
            log.info("每天时间段: {}", timeStrategy.getDailyTimes());
            log.info("每周时间段: {}", timeStrategy.getWeeklyTimes());
            
            // 检查字段是否为null
            if (timeStrategy.getDailyTimes() == null) {
                log.warn("dailyTimes 字段为 null");
            }
            if (timeStrategy.getWeeklyTimes() == null) {
                log.warn("weeklyTimes 字段为 null");
            }
        } else {
            log.info("未找到设备 {} 的时间策略", deviceId);
        }
        
        return Result.success(timeStrategy);
    }
    
    /**
     * 保存或更新时间策略
     */
    @PostMapping
    @Operation(summary = "保存时间策略", description = "保存或更新时间策略")
    public Result<Boolean> saveOrUpdate(@RequestBody TimeStrategy timeStrategy) {
        log.info("保存时间策略 - 设备ID: {}, 策略类型: {}", 
            timeStrategy.getDeviceId(), timeStrategy.getStrategyType());
        log.info("每天时间段: {}", timeStrategy.getDailyTimes());
        log.info("每周时间段: {}", timeStrategy.getWeeklyTimes());
        
        boolean success = timeStrategyService.saveOrUpdateStrategy(timeStrategy);
        return success ? Result.success(true) : Result.error("保存失败");
    }
    
    /**
     * 根据设备ID删除时间策略
     */
    @DeleteMapping("/{deviceId}")
    @Operation(summary = "删除时间策略", description = "根据设备ID删除时间策略")
    public Result<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
        boolean success = timeStrategyService.deleteByDeviceId(deviceId);
        return success ? Result.success(true) : Result.error("删除失败");
    }
    
    /**
     * 调试接口 - 检查原始数据
     */
    @GetMapping("/debug/{deviceId}")
    @Operation(summary = "调试接口", description = "调试时间策略数据")
    public Result<String> debugTimeStrategy(@PathVariable String deviceId) {
        TimeStrategy timeStrategy = timeStrategyService.getByDeviceId(deviceId);
        
        if (timeStrategy == null) {
            return Result.success("未找到数据");
        }
        
        StringBuilder debug = new StringBuilder();
        debug.append("原始对象: ").append(timeStrategy.toString()).append("\n");
        debug.append("ID: ").append(timeStrategy.getId()).append("\n");
        debug.append("设备ID: ").append(timeStrategy.getDeviceId()).append("\n");
        debug.append("策略类型: ").append(timeStrategy.getStrategyType()).append("\n");
        debug.append("每天时间段类型: ").append(timeStrategy.getDailyTimes() != null ? timeStrategy.getDailyTimes().getClass().getName() : "null").append("\n");
        debug.append("每天时间段值: ").append(timeStrategy.getDailyTimes()).append("\n");
        debug.append("每周时间段类型: ").append(timeStrategy.getWeeklyTimes() != null ? timeStrategy.getWeeklyTimes().getClass().getName() : "null").append("\n");
        debug.append("每周时间段值: ").append(timeStrategy.getWeeklyTimes()).append("\n");
        debug.append("创建时间: ").append(timeStrategy.getCreateTime()).append("\n");
        debug.append("更新时间: ").append(timeStrategy.getUpdateTime()).append("\n");
        
        return Result.success(debug.toString());
    }
} 