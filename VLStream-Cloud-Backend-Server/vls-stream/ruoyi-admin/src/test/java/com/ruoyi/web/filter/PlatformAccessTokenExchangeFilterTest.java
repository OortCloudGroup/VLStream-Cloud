/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.filter;

import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.framework.config.properties.SecurityProperties;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import com.ruoyi.web.controller.compat.tenant.MultiTenantAuthService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("dev")
class PlatformAccessTokenExchangeFilterTest {

    @Test
    void protectedRequestUsesLocalTokenAfterTransparentExchange() throws Exception {
        TokenProperties properties = new TokenProperties();
        properties.setTenantType("multi");
        SecurityProperties security = new SecurityProperties();
        security.setExcludes(new String[] {"/sso/v1/exchangeToken"});
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        MultiTenantAuthService authService = mock(MultiTenantAuthService.class);
        PlatformAccessTokenExchangeFilter filter = new PlatformAccessTokenExchangeFilter(
            properties, security, tokenUserStore, authService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/blade-system/user/info");
        request.addHeader("AccessToken", "platform-token");
        request.addHeader("tenantid", "tenant-a");
        request.addHeader("appid", "app-id");
        request.addHeader("secretkey", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> downstreamToken = new AtomicReference<String>();

        when(authService.resolveLocalToken("platform-token", "tenant-a", request)).thenReturn("local-token");
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
                downstreamToken.set(TokenHeaderResolver.resolve((javax.servlet.http.HttpServletRequest) servletRequest));
            }
        };

        filter.doFilter(request, response, chain);

        assertEquals("local-token", downstreamToken.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void exchangeFailureReturnsJsonUnauthorizedInsteadOfReachingBusinessApi() throws Exception {
        TokenProperties properties = new TokenProperties();
        properties.setTenantType("multi");
        SecurityProperties security = new SecurityProperties();
        security.setExcludes(new String[0]);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        MultiTenantAuthService authService = mock(MultiTenantAuthService.class);
        PlatformAccessTokenExchangeFilter filter = new PlatformAccessTokenExchangeFilter(
            properties, security, tokenUserStore, authService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/blade-system/user/info");
        request.addHeader("AccessToken", "bad-platform-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authService.resolveLocalToken("bad-platform-token", null, request))
            .thenThrow(new IllegalStateException("平台 token 无效"));
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("认证失败后不应进入业务接口");
        });

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(true, response.getContentAsString().contains("平台 token 无效"));
    }

    @Test
    void localEventReadsRejectUnknownCredentialsWithoutTryingPlatformExchange() throws Exception {
        TokenProperties properties = new TokenProperties(); properties.setTenantType("multi");
        SecurityProperties security = new SecurityProperties(); security.setExcludes(new String[0]);
        MultiTenantAuthService auth = mock(MultiTenantAuthService.class);
        PlatformAccessTokenExchangeFilter filter = new PlatformAccessTokenExchangeFilter(properties, security,
            mock(BladeTokenUserStore.class), auth);
        for (String path : new String[] {"/vlsEventManagement/page", "/vlsEventManagement/detail"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.addHeader("Authorization", "Bearer fixture-unknown-or-expired");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {throw new AssertionError("Must reject before business code");});
            assertEquals(401, response.getStatus());
            assertEquals(true, response.getContentAsString().contains("本地会话无效或已过期"));
        }
        verifyNoInteractions(auth);
    }

    @Test
    void knownLocalEventSessionPassesToLocalLoginChecksWithoutPlatformExchange() throws Exception {
        TokenProperties properties = new TokenProperties(); properties.setTenantType("multi");
        SecurityProperties security = new SecurityProperties(); security.setExcludes(new String[0]);
        MultiTenantAuthService auth = mock(MultiTenantAuthService.class);
        BladeTokenUserStore users = mock(BladeTokenUserStore.class);
        when(users.get("fixture-local")).thenReturn(new com.ruoyi.common.core.domain.entity.SysUser());
        PlatformAccessTokenExchangeFilter filter = new PlatformAccessTokenExchangeFilter(properties, security, users, auth);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/vlsEventManagement/page");
        request.addHeader("Authorization", "Bearer fixture-local");
        AtomicReference<String> passed = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            passed.set(TokenHeaderResolver.resolve((javax.servlet.http.HttpServletRequest) req)));
        assertEquals("fixture-local", passed.get());
        verifyNoInteractions(auth);
    }
}
