package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.TimeStrategy;

import java.util.List;

/**
 * 时间策略Service接口
 */
public interface TimeStrategyService extends IService<TimeStrategy> {
    
    /**
     * 根据设备ID获取时间策略
     * @param deviceId 设备ID
     * @return 时间策略
     */
    TimeStrategy getByDeviceId(String deviceId);
    
    /**
     * 保存或更新时间策略
     * @param timeStrategy 时间策略
     * @return 是否成功
     */
    boolean saveOrUpdateStrategy(TimeStrategy timeStrategy);
    
    /**
     * 根据设备ID删除时间策略
     * @param deviceId 设备ID
     * @return 是否成功
     */
    boolean deleteByDeviceId(String deviceId);
    
    /**
     * 获取需要执行的时间策略
     * @return 需要执行的时间策略列表
     */
    List<TimeStrategy> getTimeStrategiesToExecute();
} 