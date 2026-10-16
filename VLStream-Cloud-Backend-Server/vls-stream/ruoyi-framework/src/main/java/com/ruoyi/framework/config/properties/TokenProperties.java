/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config.properties;

import com.ruoyi.common.constant.UserConstants;
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
    private String multiTenantRoleKey = UserConstants.DEFAULT_MEMBER_ROLE_KEY;
    private String multiTenantRoleName = UserConstants.DEFAULT_MEMBER_ROLE_NAME;
    private String multiTenantRoleTemplateKey = UserConstants.DEFAULT_MEMBER_ROLE_KEY;
    private int multiTenantConnectTimeoutMillis = 3000;
    private int multiTenantReadTimeoutMillis = 5000;
}
