/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.domain.RecordEventStrategy;
import com.ruoyi.vlstream.mapper.VlsRecordEventStrategyMapper;
import com.ruoyi.vlstream.service.IVlsRecordEventStrategyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Real database service for camera record-event strategies. */
@Service
public class VlsRecordEventStrategyServiceImpl
    extends AbstractVlsTenantCrudService<VlsRecordEventStrategyMapper, RecordEventStrategy>
    implements IVlsRecordEventStrategyService {

    /** Find the non-deleted strategy associated with a device code. */
    @Override
    public RecordEventStrategy getByDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<RecordEventStrategy>()
            .eq(RecordEventStrategy::getDeviceId, deviceId.trim())
            .eq(RecordEventStrategy::getIsDeleted, Integer.valueOf(0))
            .last("limit 1"));
    }

    /** Persist the unique strategy for a device and return the stored row. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordEventStrategy saveOrUpdateStrategy(RecordEventStrategy strategy) {
        if (strategy == null || !StringUtils.hasText(strategy.getDeviceId())) {
            throw new IllegalArgumentException("Device id is required");
        }
        strategy.setDeviceId(strategy.getDeviceId().trim());
        RecordEventStrategy existing = getByDeviceId(strategy.getDeviceId());
        if (existing != null) {
            strategy.setId(existing.getId());
        }
        if (!saveOrUpdate(strategy)) {
            throw new IllegalStateException("Failed to persist record event strategy");
        }
        return getById(strategy.getId());
    }

    /** Logically delete a strategy by its device code. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            throw new IllegalArgumentException("Device id is required");
        }
        return remove(new LambdaQueryWrapper<RecordEventStrategy>()
            .eq(RecordEventStrategy::getDeviceId, deviceId.trim())) ;
    }
}
