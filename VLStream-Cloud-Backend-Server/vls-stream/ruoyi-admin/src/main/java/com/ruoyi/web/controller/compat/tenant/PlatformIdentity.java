/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import lombok.Data;

@Data
public class PlatformIdentity {
    private String userId;
    private String tenantId;
    private String userName;
    private String accessToken;
}
