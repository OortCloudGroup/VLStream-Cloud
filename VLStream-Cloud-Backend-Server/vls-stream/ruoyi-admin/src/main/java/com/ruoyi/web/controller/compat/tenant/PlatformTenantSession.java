/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlatformTenantSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private String platformAccessToken;
    private String platformUserId;
    private String tenantId;
    private PlatformGatewayHeaders gatewayHeaders;
    private List<PlatformTenant> tenants = new ArrayList<PlatformTenant>();
}
