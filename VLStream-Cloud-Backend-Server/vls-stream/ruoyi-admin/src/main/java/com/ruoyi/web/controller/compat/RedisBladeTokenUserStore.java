/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.framework.config.properties.TokenProperties;
import org.springframework.stereotype.Component;

@Component
public class RedisBladeTokenUserStore implements BladeTokenUserStore {

    private final TokenProperties tokenProperties;

    /**
     * Create a token store that normalizes all cached users to the configured tenant.
     */
    public RedisBladeTokenUserStore(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    @Override
    public SysUser get(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if (user != null && !isMultiTenant()) {
            user.setTenantId(tokenProperties.getSingleTenantId());
        }
        return user;
    }

    @Override
    public void put(String token, SysUser user, long timeoutSeconds) {
        if (user != null && !isMultiTenant()) {
            user.setTenantId(tokenProperties.getSingleTenantId());
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

    private boolean isMultiTenant() {
        return TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType());
    }
}
