package com.ruoyi.vlstream.compat;

/**
 * Single-tenant compatibility constants for the local RuoYi user system.
 */
public final class SingleTenant {

    public static final String DEFAULT_TENANT_ID = "000000";

    private SingleTenant() {
    }

    public static String orDefault(String tenantId) {
        if (tenantId == null) {
            return DEFAULT_TENANT_ID;
        }
        String trimmed = tenantId.trim();
        return trimmed.isEmpty() ? DEFAULT_TENANT_ID : trimmed;
    }
}
