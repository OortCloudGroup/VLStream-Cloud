/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.utils.redis.RedisUtils;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Component;

@Component
public class PlatformTenantSessionStore {

    private static final String KEY_PREFIX = "vls:platform-tenant-session:";
    private static final String PLATFORM_TOKEN_KEY_PREFIX = "vls:platform-token-local:";

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

    public String getLocalToken(String platformToken) {
        return RedisUtils.getCacheObject(platformTokenKey(platformToken));
    }

    public void bindPlatformToken(String platformToken, String localToken, long timeoutSeconds) {
        String key = platformTokenKey(platformToken);
        RedisUtils.setCacheObject(key, localToken);
        if (timeoutSeconds > 0) {
            RedisUtils.expire(key, timeoutSeconds);
        }
    }

    public void removePlatformToken(String platformToken) {
        RedisUtils.deleteObject(platformTokenKey(platformToken));
    }

    private String key(String localToken) {
        return KEY_PREFIX + localToken;
    }

    private String platformTokenKey(String platformToken) {
        return PLATFORM_TOKEN_KEY_PREFIX + DigestUtil.sha256Hex(platformToken);
    }
}
