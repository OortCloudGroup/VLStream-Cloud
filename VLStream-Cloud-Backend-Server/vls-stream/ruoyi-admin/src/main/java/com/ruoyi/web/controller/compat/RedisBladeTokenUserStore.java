/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.redis.RedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisBladeTokenUserStore implements BladeTokenUserStore {

    private final String singleTenantId;

    /**
     * Create a token store that normalizes all cached users to the configured tenant.
     */
    public RedisBladeTokenUserStore(@Value("${vls.tenant.id:000000}") String singleTenantId) {
        this.singleTenantId = singleTenantId;
    }

    @Override
    public SysUser get(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if (user != null) {
            user.setTenantId(singleTenantId);
        }
        return user;
    }

    @Override
    public void put(String token, SysUser user, long timeoutSeconds) {
        if (user != null) {
            user.setTenantId(singleTenantId);
        }
        RedisUtils.setCacheObject(token, user);
        if (timeoutSeconds > 0) {
            RedisUtils.expire(token, timeoutSeconds);
        }
    }

    @Override
    public void remove(String token) {
        RedisUtils.deleteObject(token);
    }
}
