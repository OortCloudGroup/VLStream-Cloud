/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class SingleTenantTest {

    @Test
    void defaultsBlankTenantToFixedSingleTenant() {
        assertEquals("000000", SingleTenant.orDefault(null));
        assertEquals("000000", SingleTenant.orDefault(""));
        assertEquals("000000", SingleTenant.orDefault("   "));
    }

    @Test
    void preservesExplicitTenantForCompatibilityOnlyWhenNonBlank() {
        assertEquals("tenant-a", SingleTenant.orDefault("tenant-a"));
    }
}
