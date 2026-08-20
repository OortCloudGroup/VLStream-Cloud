/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.utils.redis.RedisUtils;
import org.springframework.stereotype.Component;

@Component
public class PlatformTenantSessionStore {

    private static final String KEY_PREFIX = "vls:platform-tenant-session:";

    public PlatformTenantSession get(String localToken) {
        return RedisUtils.getCacheObject(key(localToken));
    }

    public void put(String localToken, PlatformTenantSession session, long timeoutSeconds) {
        String key = key(localToken);
        RedisUtils.setCacheObject(key, session);
        if (timeoutSeconds > 0) {
            RedisUtils.expire(key, timeoutSeconds);
        }
    }

    public void remove(String localToken) {
        RedisUtils.deleteObject(key(localToken));
    }

    private String key(String localToken) {
        return KEY_PREFIX + localToken;
    }
}
