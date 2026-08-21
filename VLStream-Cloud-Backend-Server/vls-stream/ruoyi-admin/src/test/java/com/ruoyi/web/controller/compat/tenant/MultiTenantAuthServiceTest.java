/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.web.controller.compat.BladeTokenSessionService;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    private static MultiTenantAuthService service(PlatformTenantClient client,
                                                   MultiTenantShadowUserService shadowUserService,
                                                   PlatformTenantSessionStore sessionStore,
                                                   BladeTokenUserStore tokenUserStore,
                                                   SysLoginService loginService) {
        return new MultiTenantAuthService(client, shadowUserService, sessionStore, tokenUserStore,
            mock(BladeTokenSessionService.class), loginService, 3600L);
    }

    private static PlatformIdentity identity(String userId, String tenantId) {
        PlatformIdentity identity = new PlatformIdentity();
        identity.setUserId(userId);
        identity.setTenantId(tenantId);
        identity.setUserName("Platform User");
        return identity;
    }
}
