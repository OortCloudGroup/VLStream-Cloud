/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.compat;

/**
 * Single-tenant compatibility constants for the local RuoYi user system.
 */
public final class SingleTenant {

    public static final String DEFAULT_TENANT_ID = "000000";

    private SingleTenant() {
    }

    /**
     * Ignore compatibility input and always return the backend-owned tenant.
     */
    public static String orDefault(String ignoredTenantId) {
        return DEFAULT_TENANT_ID;
    }
}
