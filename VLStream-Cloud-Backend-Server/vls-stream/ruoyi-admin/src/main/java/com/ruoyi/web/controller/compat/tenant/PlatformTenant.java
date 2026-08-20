/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlatformTenant implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String tenantName;
    private String userId;
    private String userName;
    private String phrase;
    private Integer status;
}
