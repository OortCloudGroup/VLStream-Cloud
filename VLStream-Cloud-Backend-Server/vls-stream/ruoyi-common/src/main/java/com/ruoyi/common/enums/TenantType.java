/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.enums;

import lombok.Generated;

public enum TenantType {
    MULTI_TENANT("多租户", "multi"),
    SIGNLE_TENANT("单租户", "single");

    final String name;
    final String type;

    public static TenantType of(String type) {
        if (type == null) {
            return null;
        } else {
            TenantType[] values = values();

            for(TenantType tenantType : values) {
                if (tenantType.type.equals(type)) {
                    return tenantType;
                }
            }

            return null;
        }
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getType() {
        return this.type;
    }

    @Generated
    private TenantType(final String name, final String type) {
        this.name = name;
        this.type = type;
    }
}
