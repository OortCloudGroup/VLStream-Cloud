/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.SysPermissionService;

import com.ruoyi.vlstream.test.compat.BladeResult;
import com.ruoyi.vlstream.test.compat.BladeUserInfoBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class BladeSystemUserCompatControllerTest {

    @Test
    void infoReturnsCachedRuoYiUserRolesPermissionsAndAliases() {
        BladeTokenUserStore tokenUserStore = mock(BladeTokenUserStore.class);
        SysPermissionService permissionService = mock(SysPermissionService.class);
        BladeSystemUserCompatController controller = new BladeSystemUserCompatController(
            tokenUserStore, permissionService, new BladeUserInfoBuilder());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sa-token");
        SysUser user = new SysUser();
        user.setUserName("admin");
        user.setTenantId("tenant-a");
        Set<String> roles = new LinkedHashSet<String>(Arrays.asList("admin"));
        Set<String> permissions = new LinkedHashSet<String>(Arrays.asList("*:*:*"));

        when(tokenUserStore.get("sa-token")).thenReturn(user);
        when(permissionService.getRolePermission(user)).thenReturn(roles);
        when(permissionService.getMenuPermission(user)).thenReturn(permissions);

        BladeResult<Map<String, Object>> result = controller.info(request);

        assertEquals(200, result.getCode());
        assertEquals(user, result.getData().get("user"));
        assertEquals(roles, result.getData().get("roles"));
        assertEquals(permissions, result.getData().get("permissions"));
        assertEquals("admin", result.getData().get("account"));
        assertEquals("admin", result.getData().get("userName"));
        assertEquals("tenant-a", result.getData().get("tenantId"));
        assertEquals("sa-token", result.getData().get("accessToken"));
    }

    @Test
    void exposesBladeSystemUserInfoRoute() throws Exception {
        RequestMapping classMapping = BladeSystemUserCompatController.class.getAnnotation(RequestMapping.class);
        Method info = BladeSystemUserCompatController.class.getDeclaredMethod("info", javax.servlet.http.HttpServletRequest.class);

        assertArrayEquals(new String[] {"/blade-system/user"}, classMapping.value());
        assertArrayEquals(new String[] {"/info"}, info.getAnnotation(GetMapping.class).value());
    }
}
