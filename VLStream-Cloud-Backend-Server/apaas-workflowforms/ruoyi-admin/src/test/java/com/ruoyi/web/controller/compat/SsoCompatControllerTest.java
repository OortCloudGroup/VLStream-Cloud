package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.vlstream.compat.BladeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class SsoCompatControllerTest {

    @Test
    void getUserTenantsReturnsCachedLocalUserAndTenantList() {
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SsoCompatController controller = new SsoCompatController(tokenUserStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("accesstoken", "sa-token");
        SysUser user = new SysUser();
        user.setUserId("user-1");
        user.setUserName("workflow-admin");
        user.setTenantId("tenant-a");

        when(tokenUserStore.get("sa-token")).thenReturn(user);

        BladeResult<Map<String, Object>> result = controller.getUserTenants(request);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tenants = (List<Map<String, Object>>) result.getData().get("list");
        Map<String, Object> firstTenant = tenants.get(0);

        assertEquals(200, result.getCode());
        assertEquals(1, tenants.size());
        assertEquals("tenant-a", firstTenant.get("tenant_id"));
        assertEquals("tenant-a", firstTenant.get("tenant_name"));
        assertEquals("user-1", firstTenant.get("user_id"));
        assertEquals("workflow-admin", firstTenant.get("user_name"));
        assertEquals(1, firstTenant.get("status"));
        assertEquals("tenant-a", firstTenant.get("phrase"));
    }

    @Test
    void getUserInfoReturnsCachedLocalUserAliasesAndToken() {
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SsoCompatController controller = new SsoCompatController(tokenUserStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("accesstoken", "sa-token");
        SysUser user = new SysUser();
        user.setUserId("user-1");
        user.setUserName("workflow-admin");
        user.setLoginId("admin");
        user.setTenantId("tenant-a");

        when(tokenUserStore.get("sa-token")).thenReturn(user);

        BladeResult<Map<String, Object>> result = controller.getUserInfo(request);

        assertEquals(200, result.getCode());
        assertEquals(user, result.getData().get("user"));
        assertEquals(user, result.getData().get("userInfo"));
        assertEquals("admin", result.getData().get("account"));
        assertEquals("workflow-admin", result.getData().get("userName"));
        assertEquals("tenant-a", result.getData().get("tenantId"));
        assertEquals("user-1", result.getData().get("user_id"));
        assertEquals("sa-token", result.getData().get("accessToken"));
    }

    @Test
    void refreshTokenReturnsCurrentAccessTokenWhenCached() {
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SsoCompatController controller = new SsoCompatController(tokenUserStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("AccessToken", "sa-token");
        SysUser user = new SysUser();
        user.setUserName("workflow-admin");
        user.setTenantId("tenant-a");

        when(tokenUserStore.get("sa-token")).thenReturn(user);

        BladeResult<Map<String, Object>> result = controller.refreshToken(request, Collections.<String, Object>emptyMap());

        assertEquals(200, result.getCode());
        assertEquals("sa-token", result.getData().get("accessToken"));
        assertEquals("sa-token", result.getData().get("refreshToken"));
        assertEquals("tenant-a", result.getData().get("tenantId"));
        assertEquals("workflow-admin", result.getData().get("userName"));
    }

    @Test
    void exposesSsoGetUserTenantsRoute() throws Exception {
        RequestMapping classMapping = SsoCompatController.class.getAnnotation(RequestMapping.class);
        Method getUserTenants = SsoCompatController.class.getDeclaredMethod("getUserTenants", javax.servlet.http.HttpServletRequest.class);

        assertArrayEquals(new String[] {"/sso/v1"}, classMapping.value());
        assertArrayEquals(new String[] {"/getUserTenants"}, getUserTenants.getAnnotation(PostMapping.class).value());
    }

    @Test
    void exposesSsoGetUserInfoRoute() throws Exception {
        Method getUserInfo = SsoCompatController.class.getDeclaredMethod("getUserInfo", javax.servlet.http.HttpServletRequest.class);

        assertArrayEquals(new String[] {"/getUserInfo"}, getUserInfo.getAnnotation(PostMapping.class).value());
    }

    @Test
    void exposesSsoRefreshTokenRoute() throws Exception {
        Method refreshToken = SsoCompatController.class.getDeclaredMethod(
            "refreshToken", javax.servlet.http.HttpServletRequest.class, Map.class);

        assertArrayEquals(new String[] {"/refreshToken"}, refreshToken.getAnnotation(PostMapping.class).value());
    }
}
