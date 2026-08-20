/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.web.controller.compat.BladeTokenSessionService;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 完成平台 token 到本地 Sa-Token 的换票以及受校验的租户切换。
 */
@Slf4j
@Service
public class MultiTenantAuthService {

    private final PlatformTenantClient platformClient;
    private final MultiTenantShadowUserService shadowUserService;
    private final PlatformTenantSessionStore platformSessionStore;
    private final BladeTokenUserStore tokenUserStore;
    private final BladeTokenSessionService tokenSessionService;
    private final SysLoginService loginService;
    private final long tokenTimeout;

    public MultiTenantAuthService(PlatformTenantClient platformClient,
                                  MultiTenantShadowUserService shadowUserService,
                                  PlatformTenantSessionStore platformSessionStore,
                                  BladeTokenUserStore tokenUserStore,
                                  BladeTokenSessionService tokenSessionService,
                                  SysLoginService loginService,
                                  @Value("${sa-token.timeout:86400}") long tokenTimeout) {
        this.platformClient = platformClient;
        this.shadowUserService = shadowUserService;
        this.platformSessionStore = platformSessionStore;
        this.tokenUserStore = tokenUserStore;
        this.tokenSessionService = tokenSessionService;
        this.loginService = loginService;
        this.tokenTimeout = tokenTimeout;
    }

    public Map<String, Object> exchange(String platformToken, String requestedTenantId,
                                        HttpServletRequest request) {
        PlatformGatewayHeaders gatewayHeaders = platformClient.resolveGatewayHeaders(request);
        List<PlatformTenant> tenants = platformClient.getUserTenants(platformToken, gatewayHeaders);
        PlatformIdentity identity = platformClient.verifyToken(platformToken, gatewayHeaders);
        String selectedPlatformToken = platformToken;

        if (StringUtils.isNotBlank(requestedTenantId)
            && !requestedTenantId.trim().equals(identity.getTenantId())) {
            requireMembership(tenants, requestedTenantId);
            selectedPlatformToken = platformClient.selectTenant(platformToken, requestedTenantId.trim(), gatewayHeaders);
            identity = platformClient.verifyToken(selectedPlatformToken, gatewayHeaders);
        }
        validateIdentity(identity, requestedTenantId, tenants);
        return createLocalSession(identity, selectedPlatformToken, gatewayHeaders, tenants);
    }

    public Map<String, Object> switchTenant(String oldLocalToken, String targetTenantId) {
        if (StringUtils.isBlank(targetTenantId)) {
            throw new IllegalArgumentException("目标租户不能为空");
        }
        PlatformTenantSession oldSession = requireSession(oldLocalToken);
        List<PlatformTenant> currentTenants = platformClient.getUserTenants(
            oldSession.getPlatformAccessToken(), oldSession.getGatewayHeaders());
        requireMembership(currentTenants, targetTenantId.trim());

        String selectedPlatformToken = platformClient.selectTenant(oldSession.getPlatformAccessToken(),
            targetTenantId.trim(), oldSession.getGatewayHeaders());
        PlatformIdentity identity = platformClient.verifyToken(selectedPlatformToken, oldSession.getGatewayHeaders());
        validateIdentity(identity, targetTenantId, currentTenants);
        Map<String, Object> result = createLocalSession(identity, selectedPlatformToken,
            oldSession.getGatewayHeaders(), currentTenants);
        tokenSessionService.logoutByToken(oldLocalToken);
        tokenUserStore.remove(oldLocalToken);
        platformSessionStore.remove(oldLocalToken);
        return result;
    }

    public PlatformTenantSession requireSession(String localToken) {
        PlatformTenantSession session = platformSessionStore.get(localToken);
        if (session == null) {
            throw new IllegalStateException("多租户平台会话不存在或已过期");
        }
        return session;
    }

    public void removeSession(String localToken) {
        platformSessionStore.remove(localToken);
    }

    private Map<String, Object> createLocalSession(PlatformIdentity identity, String platformToken,
                                                   PlatformGatewayHeaders gatewayHeaders,
                                                   List<PlatformTenant> tenants) {
        String displayName = identity.getUserName();
        try {
            String platformDisplayName = platformClient.loadDisplayName(platformToken, identity.getUserId(), gatewayHeaders);
            if (StringUtils.isNotBlank(platformDisplayName)) {
                displayName = platformDisplayName;
            }
        } catch (Exception exception) {
            log.warn("获取平台用户详情失败，继续使用 verifyToken 用户名: tenantId={}, platformUserId={}",
                identity.getTenantId(), identity.getUserId());
        }

        SysUser user = shadowUserService.loadOrCreate(identity, displayName);
        String localToken = loginService.loginPlatformUser(user);
        tokenUserStore.put(localToken, user, tokenTimeout);

        PlatformTenantSession session = new PlatformTenantSession();
        session.setPlatformAccessToken(platformToken);
        session.setPlatformUserId(identity.getUserId());
        session.setTenantId(identity.getTenantId());
        session.setGatewayHeaders(gatewayHeaders);
        session.setTenants(tenants);
        platformSessionStore.put(localToken, session, tokenTimeout);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("accessToken", localToken);
        result.put("token", localToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", tokenTimeout);
        result.put("tenantId", identity.getTenantId());
        result.put("account", user.getUserName());
        result.put("userName", user.getNickName());
        result.put("user", user);
        result.put("list", tenantMaps(tenants));
        return result;
    }

    public static List<Map<String, Object>> tenantMaps(List<PlatformTenant> tenants) {
        java.util.ArrayList<Map<String, Object>> result = new java.util.ArrayList<Map<String, Object>>();
        if (tenants == null) {
            return result;
        }
        for (PlatformTenant tenant : tenants) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("tenant_id", tenant.getTenantId());
            item.put("tenant_name", StringUtils.isBlank(tenant.getTenantName()) ? tenant.getTenantId() : tenant.getTenantName());
            item.put("user_id", tenant.getUserId());
            item.put("user_name", tenant.getUserName());
            item.put("status", tenant.getStatus() == null ? 1 : tenant.getStatus());
            item.put("phrase", tenant.getPhrase());
            result.add(item);
        }
        return result;
    }

    private static void validateIdentity(PlatformIdentity identity, String requestedTenantId,
                                         List<PlatformTenant> tenants) {
        if (StringUtils.isNotBlank(requestedTenantId)
            && !requestedTenantId.trim().equals(identity.getTenantId())) {
            throw new IllegalStateException("平台返回租户与目标租户不一致");
        }
        requireMembership(tenants, identity.getTenantId());
    }

    private static void requireMembership(List<PlatformTenant> tenants, String tenantId) {
        for (PlatformTenant tenant : tenants) {
            if (tenantId.equals(tenant.getTenantId())) {
                return;
            }
        }
        throw new IllegalStateException("目标租户不在当前平台用户的租户列表中");
    }
}
