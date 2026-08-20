/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "token")
@Component
@Data
public class TokenProperties {
    private String tenantType = "single";
    private String singleTenantId = "000000";
    private String multiTenantVerifyTokenAddress;
    private String multiTenantAdminUserUrl;
    private String multiTenantUserTenantsUrl;
    private String multiTenantLoginUrl;
    private String multiTenantRoleKey = "tenant_admin";
    private String multiTenantRoleName = "租户管理员";
    private String multiTenantRoleTemplateKey = "admin";
    private int multiTenantConnectTimeoutMillis = 3000;
    private int multiTenantReadTimeoutMillis = 5000;
}
