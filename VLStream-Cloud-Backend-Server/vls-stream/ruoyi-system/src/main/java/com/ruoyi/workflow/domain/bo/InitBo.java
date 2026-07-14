/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InitBo {
    @JsonProperty("accessToken")
    private String token;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("to_tenant_id")
    private String ToTenantId;
}
