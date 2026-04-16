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
 * Time Strategy Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/time-strategy")
@RequiredArgsConstructor
@Api(tags = "Time Strategy Management")
public class TimeStrategyController {
    
    private final TimeStrategyService timeStrategyService;
    
    /**
     * Get time strategy by device ID
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "Get time strategy", description = "Get time strategy by device ID")
    public Result<TimeStrategy> getByDeviceId(@PathVariable String deviceId) {
        log.info("Get device time strategy, device ID: {}", deviceId);
        TimeStrategy timeStrategy = timeStrategyService.getByDeviceId(deviceId);
        
        // Add debug logs
        if (timeStrategy != null) {
            log.info("Found time strategy - ID: {}, device ID: {}, strategy type: {}", 
                timeStrategy.getId(), timeStrategy.getDeviceId(), timeStrategy.getStrategyType());
            log.info("Daily time periods: {}", timeStrategy.getDailyTimes());
            log.info("Weekly time periods: {}", timeStrategy.getWeeklyTimes());
            
            // Check if fields are null
            if (timeStrategy.getDailyTimes() == null) {
                log.warn("dailyTimes field is null");
            }
            if (timeStrategy.getWeeklyTimes() == null) {
                log.warn("weeklyTimes field is null");
            }
        } else {
            log.info("Time strategy not found for device {}", deviceId);
        }
        
        return Result.success(timeStrategy);
    }
    
    /**
     * Save or update time strategy
     */
    @PostMapping
    @Operation(summary = "Save time strategy", description = "Save or update time strategy")
    public Result<Boolean> saveOrUpdate(@RequestBody TimeStrategy timeStrategy) {
        log.info("Save time strategy - device ID: {}, strategy type: {}", 
            timeStrategy.getDeviceId(), timeStrategy.getStrategyType());
        log.info("Daily time periods: {}", timeStrategy.getDailyTimes());
        log.info("Weekly time periods: {}", timeStrategy.getWeeklyTimes());
        
        boolean success = timeStrategyService.saveOrUpdateStrategy(timeStrategy);
        return success ? Result.success(true) : Result.error("Save failed");
    }

    /**
     * Delete time strategy by device ID
     */
    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete time strategy", description = "Delete time strategy by device ID")
    public Result<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
        boolean success = timeStrategyService.deleteByDeviceId(deviceId);
        return success ? Result.success(true) : Result.error("Delete failed");
    }

    /**
     * Debug interface - Check raw data
     */
    @GetMapping("/debug/{deviceId}")
    @Operation(summary = "Debug interface", description = "Debug time strategy data")
    public Result<String> debugTimeStrategy(@PathVariable String deviceId) {
        TimeStrategy timeStrategy = timeStrategyService.getByDeviceId(deviceId);
        
        if (timeStrategy == null) {
            return Result.success("Data not found");
        }
        
        StringBuilder debug = new StringBuilder();
        debug.append("Original object: ").append(timeStrategy.toString()).append("\n");
        debug.append("ID: ").append(timeStrategy.getId()).append("\n");
        debug.append("Device ID: ").append(timeStrategy.getDeviceId()).append("\n");
        debug.append("Strategy type: ").append(timeStrategy.getStrategyType()).append("\n");
        debug.append("Daily time periods type: ").append(timeStrategy.getDailyTimes() != null ? timeStrategy.getDailyTimes().getClass().getName() : "null").append("\n");
        debug.append("Daily time periods value: ").append(timeStrategy.getDailyTimes()).append("\n");
        debug.append("Weekly time periods type: ").append(timeStrategy.getWeeklyTimes() != null ? timeStrategy.getWeeklyTimes().getClass().getName() : "null").append("\n");
        debug.append("Weekly time periods value: ").append(timeStrategy.getWeeklyTimes()).append("\n");
        debug.append("Create time: ").append(timeStrategy.getCreateTime()).append("\n");
        debug.append("Update time: ").append(timeStrategy.getUpdateTime()).append("\n");
        
        return Result.success(debug.toString());
    }
} 