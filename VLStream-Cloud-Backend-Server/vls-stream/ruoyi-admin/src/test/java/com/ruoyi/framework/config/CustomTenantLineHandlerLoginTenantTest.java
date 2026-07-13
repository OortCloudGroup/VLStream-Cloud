/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import com.ruoyi.common.helper.TenantContextHolder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class CustomTenantLineHandlerLoginTenantTest {

    @Test
    void usesBoundTenantBeforeTokenExists() {
        CustomTenantLineHandler handler = new CustomTenantLineHandler();
        ReflectionTestUtils.setField(handler, "tenantType", "multi");
        ReflectionTestUtils.setField(handler, "singleTenantId", "single-tenant");

        TenantContextHolder.setTenantId("tenant-from-login");
        try {
            assertTrue(handler.getTenantId().toString().contains("tenant-from-login"));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void ignoresLocalSysUserShadowTableWithoutTenantColumn() {
        CustomTenantLineHandler handler = new CustomTenantLineHandler();

        assertTrue(handler.ignoreTable("sys_user"));
    }
}
