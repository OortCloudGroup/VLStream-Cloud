/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.helper.TenantContextHolder;

import java.util.function.Supplier;

final class TunnelTenantScope {
    private TunnelTenantScope() {
    }

    static <T> T call(String tenantId, Supplier<T> action) {
        String previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            return action.get();
        } finally {
            TenantContextHolder.setTenantId(previous);
        }
    }

    static void run(String tenantId, Runnable action) {
        call(tenantId, new Supplier<Object>() {
            @Override
            public Object get() {
                action.run();
                return null;
            }
        });
    }
}
