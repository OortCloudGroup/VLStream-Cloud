/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.framework.config.properties.TokenProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class CustomTenantLineHandlerLoginTenantTest {

    @Test
    void usesBoundTenantBeforeTokenExists() {
        TokenProperties properties = new TokenProperties();
        properties.setTenantType("multi");
        properties.setSingleTenantId("single-tenant");
        CustomTenantLineHandler handler = new CustomTenantLineHandler(properties);

        TenantContextHolder.setTenantId("tenant-from-login");
        try {
            assertTrue(handler.getTenantId().toString().contains("tenant-from-login"));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void ignoresUnknownTableWithoutTenantColumn() {
        CustomTenantLineHandler handler = new CustomTenantLineHandler(new TokenProperties());

        assertTrue(handler.ignoreTable("unknown_table"));
    }

    @Test
    void singleTenantModeUsesConfiguredDefaultTenant() {
        TokenProperties properties = new TokenProperties();
        properties.setTenantType("single");
        properties.setSingleTenantId("000000");
        CustomTenantLineHandler handler = new CustomTenantLineHandler(properties);

        assertEquals("000000", handler.resolveTenantId());
    }
}
