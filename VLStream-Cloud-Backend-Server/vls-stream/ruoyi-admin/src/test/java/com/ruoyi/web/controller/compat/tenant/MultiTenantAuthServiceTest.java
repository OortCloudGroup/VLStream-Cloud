/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.web.controller.compat.BladeTokenSessionService;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class MultiTenantAuthServiceTest {

    @Test
    void currentTenantExchangeContinuesWhenTenantListIsTemporarilyUnavailable() {
        PlatformTenantClient client = mock(PlatformTenantClient.class);
        MultiTenantShadowUserService shadowUserService = mock(MultiTenantShadowUserService.class);
        PlatformTenantSessionStore sessionStore = mock(PlatformTenantSessionStore.class);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SysLoginService loginService = mock(SysLoginService.class);
        PlatformGatewayHeaders headers = new PlatformGatewayHeaders("app", "app-id", "secret");
        PlatformIdentity identity = identity("user-1", "tenant-a");
        SysUser user = new SysUser();
        user.setUserName("platform-user");
        user.setTenantId("tenant-a");
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(client.resolveGatewayHeaders(request)).thenReturn(headers);
        when(client.verifyToken("platform-token", headers)).thenReturn(identity);
        when(client.getUserTenants("platform-token", headers))
            .thenThrow(new IllegalStateException("平台接口 HTTP 状态异常: 504"));
        when(client.loadDisplayName("platform-token", "user-1", headers)).thenReturn("Platform User");
        when(shadowUserService.loadOrCreate(eq(identity), any())).thenReturn(user);
        when(loginService.loginPlatformUser(user)).thenReturn("local-token");

        MultiTenantAuthService service = service(client, shadowUserService, sessionStore,
            tokenUserStore, loginService);
        Map<String, Object> result = service.exchange("platform-token", "tenant-a", request);

        assertEquals("local-token", result.get("accessToken"));
        assertEquals("tenant-a", result.get("tenantId"));
        assertEquals(1, ((java.util.List<?>) result.get("list")).size());
        verify(sessionStore).bindPlatformToken("platform-token", "local-token", 3600L);
    }

    @Test
    void resolveLocalTokenReusesCachedSessionForMatchingTenant() {
        PlatformTenantClient client = mock(PlatformTenantClient.class);
        MultiTenantShadowUserService shadowUserService = mock(MultiTenantShadowUserService.class);
        PlatformTenantSessionStore sessionStore = mock(PlatformTenantSessionStore.class);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SysLoginService loginService = mock(SysLoginService.class);
        PlatformTenantSession session = new PlatformTenantSession();
        session.setTenantId("tenant-a");
        SysUser user = new SysUser();

        when(sessionStore.getLocalToken("platform-token")).thenReturn("local-token");
        when(sessionStore.get("local-token")).thenReturn(session);
        when(tokenUserStore.get("local-token")).thenReturn(user);

        MultiTenantAuthService service = service(client, shadowUserService, sessionStore,
            tokenUserStore, loginService);

        assertEquals("local-token", service.resolveLocalToken("platform-token", "tenant-a",
            new MockHttpServletRequest()));
    }

    @Test
    void singleTenantExchangeMapsPlatformUserToFixedLocalTenant() {
        PlatformTenantClient client = mock(PlatformTenantClient.class);
        MultiTenantShadowUserService shadowUserService = mock(MultiTenantShadowUserService.class);
        PlatformTenantSessionStore sessionStore = mock(PlatformTenantSessionStore.class);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SysLoginService loginService = mock(SysLoginService.class);
        PlatformGatewayHeaders headers = new PlatformGatewayHeaders("app", "app-id", "secret");
        PlatformIdentity identity = identity("user-1", "platform-tenant");
        SysUser user = new SysUser();
        user.setUserName("platform-user");
        user.setTenantId("000000");
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(client.resolveGatewayHeaders(request)).thenReturn(headers);
        when(client.verifyToken("platform-token", headers)).thenReturn(identity);
        when(client.loadDisplayName("platform-token", "user-1", headers)).thenReturn("Platform User");
        when(shadowUserService.loadOrCreate(eq(identity), any())).thenReturn(user);
        when(loginService.loginPlatformUser(user)).thenReturn("local-token");

        MultiTenantAuthService service = service(client, shadowUserService, sessionStore,
            tokenUserStore, loginService);
        Map<String, Object> result = service.exchangeForSingleTenant("platform-token", request);

        assertEquals("000000", result.get("tenantId"));
        verify(client, never()).getUserTenants(any(), any());
    }

    @Test
    void validatePlatformSessionRevalidatesBoundPlatformToken() {
        PlatformTenantClient client = mock(PlatformTenantClient.class);
        PlatformTenantSessionStore sessionStore = mock(PlatformTenantSessionStore.class);
        PlatformGatewayHeaders headers = new PlatformGatewayHeaders("app", "app-id", "secret");
        PlatformTenantSession session = new PlatformTenantSession();
        session.setPlatformAccessToken("platform-token");
        session.setPlatformUserId("user-1");
        session.setTenantId("tenant-a");
        session.setGatewayHeaders(headers);
        when(sessionStore.get("local-token")).thenReturn(session);
        when(client.verifyToken("platform-token", headers)).thenReturn(identity("user-1", "tenant-a"));

        MultiTenantAuthService service = service(client, mock(MultiTenantShadowUserService.class), sessionStore,
            mock(BladeTokenUserStore.class), mock(SysLoginService.class));

        Map<String, Object> result = service.validatePlatformSession("local-token");

        assertEquals(true, result.get("valid"));
        assertEquals("tenant-a", result.get("tenantId"));
        verify(client).verifyToken("platform-token", headers);
    }

    @Test
    void validatePlatformSessionRejectsChangedIdentity() {
        PlatformTenantClient client = mock(PlatformTenantClient.class);
        PlatformTenantSessionStore sessionStore = mock(PlatformTenantSessionStore.class);
        PlatformGatewayHeaders headers = new PlatformGatewayHeaders("app", "app-id", "secret");
        PlatformTenantSession session = new PlatformTenantSession();
        session.setPlatformAccessToken("platform-token");
        session.setPlatformUserId("user-1");
        session.setTenantId("tenant-a");
        session.setGatewayHeaders(headers);
        when(sessionStore.get("local-token")).thenReturn(session);
        when(client.verifyToken("platform-token", headers)).thenReturn(identity("user-2", "tenant-a"));

        MultiTenantAuthService service = service(client, mock(MultiTenantShadowUserService.class), sessionStore,
            mock(BladeTokenUserStore.class), mock(SysLoginService.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.validatePlatformSession("local-token"));
        assertEquals("平台会话身份已变更，请重新登录", exception.getMessage());
    }

    private static MultiTenantAuthService service(PlatformTenantClient client,
                                                   MultiTenantShadowUserService shadowUserService,
                                                   PlatformTenantSessionStore sessionStore,
                                                   BladeTokenUserStore tokenUserStore,
                                                   SysLoginService loginService) {
        TokenProperties properties = new TokenProperties();
        properties.setSingleTenantId("000000");
        return new MultiTenantAuthService(client, shadowUserService, sessionStore, tokenUserStore,
            mock(BladeTokenSessionService.class), loginService, properties, 3600L);
    }

    private static PlatformIdentity identity(String userId, String tenantId) {
        PlatformIdentity identity = new PlatformIdentity();
        identity.setUserId(userId);
        identity.setTenantId(tenantId);
        identity.setUserName("Platform User");
        return identity;
    }
}
