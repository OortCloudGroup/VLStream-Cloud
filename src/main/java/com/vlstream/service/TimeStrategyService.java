package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.TimeStrategy;

import java.util.List;

/**
 * Time strategy service interface
 */
public interface TimeStrategyService extends IService<TimeStrategy> {
    
    /**
     * Get time strategy by device ID
     * @param deviceId Device ID
     * @return Time strategy
     */
    TimeStrategy getByDeviceId(String deviceId);
    
    /**
     * Save or update time strategy
     * @param timeStrategy Time strategy
     * @return Whether successful
     */
    boolean saveOrUpdateStrategy(TimeStrategy timeStrategy);
    
    /**
     * Delete time strategy by device ID
     * @param deviceId Device ID
     * @return Whether successful
     */
    boolean deleteByDeviceId(String deviceId);
    
    /**
     * Get time strategies to execute
     * @return List of time strategies to execute
     */
    List<TimeStrategy> getTimeStrategiesToExecute();
} 