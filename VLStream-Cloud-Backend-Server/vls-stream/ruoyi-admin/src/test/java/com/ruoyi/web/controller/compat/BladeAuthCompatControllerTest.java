/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.vlstream.compat.BladeAuthInfo;
import com.ruoyi.vlstream.compat.BladePasswordDecoder;
import com.ruoyi.vlstream.compat.BladeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class BladeAuthCompatControllerTest {

    @Test
    void tokenDecryptsPasswordAndReturnsBladeAuthInfo() {
        SysLoginService loginService = mock(SysLoginService.class);
        ISysUserService userService = mock(ISysUserService.class);
        BladePasswordDecoder passwordDecoder = mock(BladePasswordDecoder.class);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        BladeTokenSessionService tokenSessionService = mock(BladeTokenSessionService.class);
        BladeAuthCompatController controller = new BladeAuthCompatController(
            loginService, userService, passwordDecoder, tokenUserStore, tokenSessionService, 86400L);
        Map<String, String> params = new HashMap<String, String>();
        params.put("grantType", "password");
        params.put("tenantId", "tenant-a");
        params.put("account", "admin");
        params.put("password", "encrypted-password");
        SysUser user = new SysUser();
        user.setUserName("admin");
        user.setTenantId("tenant-a");

        when(passwordDecoder.decode("encrypted-password")).thenReturn("plain-password");
        when(loginService.login("admin", "plain-password", null, null)).thenReturn("sa-token");
        when(userService.selectUserByUserName("admin")).thenReturn(user);

        BladeResult<BladeAuthInfo> result = controller.token(params);

        assertEquals(200, result.getCode());
        assertEquals("sa-token", result.getData().getAccessToken());
        assertEquals("sa-token", result.getData().getToken());
        assertEquals("Bearer", result.getData().getTokenType());
        assertEquals("admin", result.getData().getAccount());
        assertEquals("admin", result.getData().getUserName());
        assertEquals("000000", result.getData().getTenantId());
        assertEquals(86400L, result.getData().getExpiresIn());
        verify(loginService).login("admin", "plain-password", null, null);
        verify(tokenUserStore).put("sa-token", user, 86400L);
    }

    @Test
    void tokenFailsWhenLocalUserIsMissingAfterLogin() {
        SysLoginService loginService = mock(SysLoginService.class);
        ISysUserService userService = mock(ISysUserService.class);
        BladePasswordDecoder passwordDecoder = mock(BladePasswordDecoder.class);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        BladeTokenSessionService tokenSessionService = mock(BladeTokenSessionService.class);
        BladeAuthCompatController controller = new BladeAuthCompatController(
            loginService, userService, passwordDecoder, tokenUserStore, tokenSessionService, 86400L);
        Map<String, String> params = new HashMap<String, String>();
        params.put("grantType", "password");
        params.put("tenantId", "tenant-a");
        params.put("account", "ghost");
        params.put("password", "encrypted-password");

        when(passwordDecoder.decode("encrypted-password")).thenReturn("plain-password");
        when(loginService.login("ghost", "plain-password", null, null)).thenReturn("sa-token");
        when(userService.selectUserByUserName("ghost")).thenReturn(null);

        BladeResult<BladeAuthInfo> result = controller.token(params);

        assertEquals(500, result.getCode());
        verify(tokenUserStore, never()).put(anyString(), any(SysUser.class), anyLong());
    }

    @Test
    void logoutUsesCompatibleTokenHeader() {
        SysLoginService loginService = mock(SysLoginService.class);
        ISysUserService userService = mock(ISysUserService.class);
        BladePasswordDecoder passwordDecoder = mock(BladePasswordDecoder.class);
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        BladeTokenSessionService tokenSessionService = mock(BladeTokenSessionService.class);
        BladeAuthCompatController controller = new BladeAuthCompatController(
            loginService, userService, passwordDecoder, tokenUserStore, tokenSessionService, 86400L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("blade-auth", "Bearer sa-token");

        BladeResult<Void> result = controller.logout(request);

        assertEquals(200, result.getCode());
        verify(tokenSessionService).logoutByToken("sa-token");
        verify(tokenUserStore).remove("sa-token");
    }

    @Test
    void exposesBladeAuthRoutes() throws Exception {
        RequestMapping classMapping = BladeAuthCompatController.class.getAnnotation(RequestMapping.class);
        Method token = BladeAuthCompatController.class.getDeclaredMethod("token", Map.class);
        Method logout = BladeAuthCompatController.class.getDeclaredMethod("logout", javax.servlet.http.HttpServletRequest.class);

        assertArrayEquals(new String[] {"/blade-auth"}, classMapping.value());
        assertArrayEquals(new String[] {"/token"}, token.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/logout"}, logout.getAnnotation(PostMapping.class).value());
    }
}
