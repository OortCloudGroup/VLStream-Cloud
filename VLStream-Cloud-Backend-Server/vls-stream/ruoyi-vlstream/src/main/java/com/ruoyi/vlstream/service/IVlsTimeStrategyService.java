/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

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
