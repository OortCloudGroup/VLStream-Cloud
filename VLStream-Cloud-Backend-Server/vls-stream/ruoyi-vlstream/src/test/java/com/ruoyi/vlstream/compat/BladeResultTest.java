/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class BladeResultTest {

    @Test
    void successContainsBladeFields() {
        BladeResult<String> result = BladeResult.success("ok");

        assertEquals(200, result.getCode());
        assertTrue(result.isSuccess());
        assertEquals("操作成功", result.getMsg());
        assertEquals("ok", result.getData());
    }

    @Test
    void pageUsesSpringBladeFieldNames() {
        BladePage<String> page = BladePage.of(Arrays.asList("a", "b"), 9L, 10L, 2L);

        assertEquals(Arrays.asList("a", "b"), page.getRecords());
        assertEquals(9L, page.getTotal());
        assertEquals(10L, page.getSize());
        assertEquals(2L, page.getCurrent());
    }

    @Test
    void authInfoExposesAccessTokenAndAccount() {
        BladeAuthInfo info = BladeAuthInfo.passwordToken("token-value", "admin", "Admin", "tenant-a", 86400L);

        assertEquals("token-value", info.getAccessToken());
        assertEquals("token-value", info.getToken());
        assertEquals("Bearer", info.getTokenType());
        assertEquals("admin", info.getAccount());
        assertEquals("Admin", info.getUserName());
        assertEquals("tenant-a", info.getTenantId());
        assertEquals(86400L, info.getExpiresIn());
    }
}
