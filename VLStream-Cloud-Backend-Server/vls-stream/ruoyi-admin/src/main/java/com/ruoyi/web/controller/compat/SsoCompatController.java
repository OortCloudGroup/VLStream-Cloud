/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.vlstream.test.compat.BladeResult;
import com.ruoyi.web.controller.compat.tenant.MultiTenantAuthService;
import com.ruoyi.web.controller.compat.tenant.PlatformTenantSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sso/v1")
public class SsoCompatController {

    private final BladeTokenUserStore tokenUserStore;
    private final TokenProperties tokenProperties;
    private final MultiTenantAuthService multiTenantAuthService;

    public SsoCompatController(BladeTokenUserStore tokenUserStore,
                               TokenProperties tokenProperties,
                               MultiTenantAuthService multiTenantAuthService) {
        this.tokenUserStore = tokenUserStore;
        this.tokenProperties = tokenProperties;
        this.multiTenantAuthService = multiTenantAuthService;
    }

    @GetMapping("/mode")
    public BladeResult<Map<String, Object>> mode() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("tenantType", isMultiTenant() ? "multi" : "single");
        data.put("multiTenant", isMultiTenant());
        return BladeResult.success(data);
    }

    @PostMapping("/exchangeToken")
    public BladeResult<Map<String, Object>> exchangeToken(HttpServletRequest request,
                                                           @RequestBody(required = false) Map<String, Object> body) {
        if (!isMultiTenant()) {
            return BladeResult.fail("当前为单租户模式，不支持平台换票");
        }
        String platformToken = firstNonBlank(TokenHeaderResolver.resolve(request), bodyValue(body, "accessToken"));
        if (platformToken == null) {
            return BladeResult.fail("缺少平台访问令牌");
        }
        try {
            String tenantId = firstNonBlank(bodyValue(body, "tenantId"), bodyValue(body, "tenant_id"));
            return BladeResult.success(multiTenantAuthService.exchange(platformToken, tenantId, request));
        } catch (Exception exception) {
            return BladeResult.fail(firstNonBlank(exception.getMessage(), "平台换票失败"));
        }
    }

    @PostMapping("/switchTenant")
    public BladeResult<Map<String, Object>> switchTenant(HttpServletRequest request,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        if (!isMultiTenant()) {
            return BladeResult.fail("当前为单租户模式，不支持切换租户");
        }
        String localToken = TokenHeaderResolver.resolve(request);
        String tenantId = firstNonBlank(bodyValue(body, "tenantId"), bodyValue(body, "tenant_id"));
        if (localToken == null || tenantId == null) {
            return BladeResult.fail("缺少本地令牌或目标租户");
        }
        try {
            return BladeResult.success(multiTenantAuthService.switchTenant(localToken, tenantId));
        } catch (Exception exception) {
            return BladeResult.fail(firstNonBlank(exception.getMessage(), "切换租户失败"));
        }
    }

    @PostMapping("/getUserTenants")
    public BladeResult<Map<String, Object>> getUserTenants(HttpServletRequest request) {
        String token = TokenHeaderResolver.resolve(request);
        if (token == null) {
            return BladeResult.fail("缺少访问令牌");
        }

        SysUser user = tokenUserStore.get(token);
        if (user == null) {
            return BladeResult.fail("未找到用户缓存信息");
        }

        if (isMultiTenant()) {
            try {
                PlatformTenantSession session = multiTenantAuthService.requireSession(token);
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("list", MultiTenantAuthService.tenantMaps(session.getTenants()));
                data.put("user", user);
                data.put("accessToken", token);
                data.put("token", token);
                data.put("tenantId", user.getTenantId());
                return BladeResult.success(data);
            } catch (Exception exception) {
                return BladeResult.fail(firstNonBlank(exception.getMessage(), "多租户平台会话已失效"));
            }
        }

        Map<String, Object> tenant = new LinkedHashMap<String, Object>();
        String tenantId = firstNonBlank(user.getTenantId(), tokenProperties.getSingleTenantId());
        String userName = firstNonBlank(user.getUserName(), user.getLoginId());
        tenant.put("tenant_id", tenantId);
        tenant.put("tenant_name", tenantId);
        tenant.put("user_id", user.getUserId());
        tenant.put("user_name", userName);
        tenant.put("status", 1);
        tenant.put("phrase", tenantId);

        List<Map<String, Object>> tenants = new ArrayList<Map<String, Object>>();
        tenants.add(tenant);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("list", tenants);
        data.put("user", user);
        data.put("accessToken", token);
        data.put("token", token);
        return BladeResult.success(data);
    }

    /**
     * Return cached user information in the legacy SSO envelope expected by VLStream UI.
     */
    @PostMapping("/getUserInfo")
    public BladeResult<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        String token = TokenHeaderResolver.resolve(request);
        if (token == null) {
            return BladeResult.fail("缺少访问令牌");
        }

        SysUser user = tokenUserStore.get(token);
        if (user == null) {
            return BladeResult.fail("未找到用户缓存信息");
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        String account = firstNonBlank(user.getLoginId(), user.getUserName());
        String userName = firstNonBlank(user.getUserName(), user.getLoginId());
        data.put("user", user);
        data.put("userInfo", user);
        data.put("account", account);
        data.put("userName", userName);
        data.put("tenantId", firstNonBlank(user.getTenantId(), tokenProperties.getSingleTenantId()));
        data.put("user_id", user.getUserId());
        data.put("user_name", userName);
        data.put("accessToken", token);
        data.put("token", token);
        return BladeResult.success(data);
    }

    /**
     * Reissue the current local token in the legacy SSO refresh response shape.
     */
    @PostMapping("/refreshToken")
    public BladeResult<Map<String, Object>> refreshToken(HttpServletRequest request,
                                                         @RequestBody(required = false) Map<String, Object> body) {
        String token = firstNonBlank(TokenHeaderResolver.resolve(request), bodyValue(body, "refreshToken"));
        token = firstNonBlank(token, bodyValue(body, "accessToken"));
        if (token == null) {
            return BladeResult.fail("缺少刷新令牌");
        }

        SysUser user = tokenUserStore.get(token);
        if (user == null) {
            return BladeResult.fail("未找到用户缓存信息");
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        String userName = firstNonBlank(user.getUserName(), user.getLoginId());
        data.put("accessToken", token);
        data.put("refreshToken", token);
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("user", user);
        data.put("userName", userName);
        data.put("tenantId", firstNonBlank(user.getTenantId(), tokenProperties.getSingleTenantId()));
        return BladeResult.success(data);
    }

    private boolean isMultiTenant() {
        return TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType());
    }

    private static String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : trimToNull(second);
    }

    private static String bodyValue(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
