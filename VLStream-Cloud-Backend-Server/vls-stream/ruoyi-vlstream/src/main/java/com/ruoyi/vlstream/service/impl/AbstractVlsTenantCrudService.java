/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.vlstream.domain.VlsTenantEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * Applies the fixed single-tenant and audit defaults to migrated VLS CRUD services.
 */
public abstract class AbstractVlsTenantCrudService<M extends BaseMapper<T>, T extends VlsTenantEntity>
    extends ServiceImpl<M, T> {

    protected static final String DEFAULT_TENANT_ID = "000000";

    /** Persist a new VLS business row with single-tenant defaults. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(T entity) {
        prepare(entity, true);
        return super.save(entity);
    }

    /** Update a VLS business row while preserving its creation audit fields. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(T entity) {
        prepare(entity, false);
        return super.updateById(entity);
    }

    /** Insert or update a VLS business row according to its primary key. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Business data is required");
        }
        return entity.getId() == null || getById(entity.getId()) == null
            ? save(entity)
            : updateById(entity);
    }

    /** Apply stable single-tenant, status, deletion and timestamp defaults. */
    protected void prepare(T entity, boolean create) {
        if (entity == null) {
            throw new IllegalArgumentException("Business data is required");
        }
        if (!StringUtils.hasText(entity.getTenantId())) {
            entity.setTenantId(DEFAULT_TENANT_ID);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(Integer.valueOf(1));
        }
        if (entity.getIsDeleted() == null) {
            entity.setIsDeleted(Integer.valueOf(0));
        }
        Date now = new Date();
        if (create && entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
    }
}
