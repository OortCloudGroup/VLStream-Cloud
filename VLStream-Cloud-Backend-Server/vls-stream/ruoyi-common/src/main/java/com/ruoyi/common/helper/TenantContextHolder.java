/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.helper;

import com.ruoyi.common.utils.StringUtils;

public final class TenantContextHolder {
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<String>();

    private TenantContextHolder() {
    }

    public static void setTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(tenantId.trim());
        }
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
