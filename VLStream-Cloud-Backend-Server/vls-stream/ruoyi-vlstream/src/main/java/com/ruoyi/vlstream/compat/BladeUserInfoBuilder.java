/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds the Blade-compatible user info payload from the workflows user model.
 */
@Component
public class BladeUserInfoBuilder {

    public Map<String, Object> build(String token, SysUser user, Set<String> roles, Set<String> permissions) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        String account = firstNonBlank(user.getUserName(), user.getLoginId());
        String displayName = firstNonBlank(user.getUserName(), account);

        info.put("user", user);
        info.put("roles", roles);
        info.put("permissions", permissions);
        info.put("account", account);
        info.put("userName", displayName);
        info.put("realName", displayName);
        info.put("tenantId", user.getTenantId());
        info.put("accessToken", token);
        info.put("token", token);
        info.put("tokenType", "Bearer");
        return info;
    }

    private static String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : trimToNull(second);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
