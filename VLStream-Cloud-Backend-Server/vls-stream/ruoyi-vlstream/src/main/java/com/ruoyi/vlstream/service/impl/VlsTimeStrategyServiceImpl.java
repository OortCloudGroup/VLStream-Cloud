/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.domain.TimeStrategy;
import com.ruoyi.vlstream.mapper.VlsTimeStrategyMapper;
import com.ruoyi.vlstream.service.IVlsTimeStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * Service implementation for VLS time strategy compatibility routes.
 */
@Service
@RequiredArgsConstructor
public class VlsTimeStrategyServiceImpl implements IVlsTimeStrategyService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final VlsTimeStrategyMapper timeStrategyMapper;

    @Override
    public TimeStrategy getTimeStrategy(String deviceId) {
        TimeStrategy existing = findByDeviceId(deviceId);
        TimeStrategy result = existing == null ? defaultStrategy(deviceId) : existing;
        result.fillFrontendAliases();
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TimeStrategy saveTimeStrategy(TimeStrategy timeStrategy) {
        if (timeStrategy == null || !StringUtils.hasText(timeStrategy.getDeviceId())) {
            throw new IllegalArgumentException("Device id is required");
        }
        TimeStrategy existing = findByDeviceId(timeStrategy.getDeviceId());
        normalizeDefaults(timeStrategy, existing == null);
        timeStrategy.normalizeProtectionTime();
        if (existing == null) {
            timeStrategyMapper.insert(timeStrategy);
        } else {
            timeStrategy.setId(existing.getId());
            if (timeStrategy.getCreateTime() == null) {
                timeStrategy.setCreateTime(existing.getCreateTime());
            }
            if (timeStrategy.getCreateUser() == null) {
                timeStrategy.setCreateUser(existing.getCreateUser());
            }
            if (!StringUtils.hasText(timeStrategy.getCreateDept())) {
                timeStrategy.setCreateDept(existing.getCreateDept());
            }
            timeStrategyMapper.updateById(timeStrategy);
        }
        TimeStrategy stored = timeStrategyMapper.selectById(timeStrategy.getId());
        TimeStrategy result = stored == null ? timeStrategy : stored;
        result.fillFrontendAliases();
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTimeStrategy(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return false;
        }
        return timeStrategyMapper.delete(new LambdaQueryWrapper<TimeStrategy>()
            .eq(TimeStrategy::getDeviceId, deviceId.trim())
            .eq(TimeStrategy::getIsDeleted, 0)) > 0;
    }

    private TimeStrategy findByDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return timeStrategyMapper.selectOne(new LambdaQueryWrapper<TimeStrategy>()
            .eq(TimeStrategy::getDeviceId, deviceId.trim())
            .eq(TimeStrategy::getIsDeleted, 0)
            .last("limit 1"));
    }

    private TimeStrategy defaultStrategy(String deviceId) {
        TimeStrategy strategy = new TimeStrategy();
        strategy.setDeviceId(deviceId);
        strategy.setTenantId(DEFAULT_TENANT_ID);
        strategy.setStatus(Integer.valueOf(1));
        strategy.setIsDeleted(Integer.valueOf(0));
        strategy.setStrategyType("everyday");
        strategy.normalizeProtectionTime();
        return strategy;
    }

    private void normalizeDefaults(TimeStrategy timeStrategy, boolean create) {
        timeStrategy.setDeviceId(timeStrategy.getDeviceId().trim());
        if (!StringUtils.hasText(timeStrategy.getTenantId())) {
            timeStrategy.setTenantId(DEFAULT_TENANT_ID);
        }
        if (timeStrategy.getStatus() == null) {
            timeStrategy.setStatus(Integer.valueOf(1));
        }
        if (timeStrategy.getIsDeleted() == null) {
            timeStrategy.setIsDeleted(Integer.valueOf(0));
        }
        Date now = new Date();
        if (create && timeStrategy.getCreateTime() == null) {
            timeStrategy.setCreateTime(now);
        }
        timeStrategy.setUpdateTime(now);
    }
}
