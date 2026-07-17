/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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

    /**
     * 单租户模式不应再给任何业务表拼接租户过滤条件，历史空租户数据也必须可见。
     */
    @Test
    void ignoresEveryTableInSingleTenantMode() {
        CustomTenantLineHandler handler = new CustomTenantLineHandler();

        assertTrue(handler.ignoreTable("vls_device_info"));
        assertTrue(handler.ignoreTable("oort_task_event"));
    }
}
