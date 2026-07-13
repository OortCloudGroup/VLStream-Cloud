package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.domain.TimeStrategy;

/**
 * Service for the VLS time strategy frontend compatibility surface.
 */
public interface IVlsTimeStrategyService {

    /**
     * Return a strategy by frontend device id.
     */
    TimeStrategy getTimeStrategy(String deviceId);

    /**
     * Save or update one strategy by device id.
     */
    TimeStrategy saveTimeStrategy(TimeStrategy timeStrategy);

    /**
     * Delete a strategy by device id.
     */
    boolean deleteTimeStrategy(String deviceId);
}
