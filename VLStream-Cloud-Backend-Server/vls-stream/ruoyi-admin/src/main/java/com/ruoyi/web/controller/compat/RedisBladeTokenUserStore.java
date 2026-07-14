/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.redis.RedisUtils;
import org.springframework.stereotype.Component;

@Component
public class RedisBladeTokenUserStore implements BladeTokenUserStore {

    @Override
    public SysUser get(String token) {
        return RedisUtils.getCacheObject(token);
    }

    @Override
    public void put(String token, SysUser user, long timeoutSeconds) {
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
