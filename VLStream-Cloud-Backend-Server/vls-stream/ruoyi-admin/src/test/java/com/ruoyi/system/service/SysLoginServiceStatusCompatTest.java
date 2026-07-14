/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class SysLoginServiceStatusCompatTest {

    @Test
    void ruoyiDisabledStatusIsBlocked() {
        assertTrue(SysLoginService.isLoginBlockedStatus("1"));
    }

    @Test
    void literalDisabledStatusIsBlocked() {
        assertTrue(SysLoginService.isLoginBlockedStatus("disabled"));
    }

    @Test
    void ruoyiActiveStatusIsNotBlocked() {
        assertFalse(SysLoginService.isLoginBlockedStatus("0"));
    }
}
