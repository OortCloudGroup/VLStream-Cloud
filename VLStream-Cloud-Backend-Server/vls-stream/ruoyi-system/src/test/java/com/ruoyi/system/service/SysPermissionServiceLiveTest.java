package com.ruoyi.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class SysPermissionServiceLiveTest {
    @Test
    void resolvesCurrentAssignmentsInsteadOfSessionSnapshot() {
        ISysRoleService roles = mock(ISysRoleService.class);
        ISysMenuService menus = mock(ISysMenuService.class);
        SysPermissionService service = new SysPermissionService(roles, menus);
        when(menus.selectMenuPermsByUserId("42"))
                .thenReturn(Collections.singleton("vls:menu:algorithm-management"))
                .thenReturn(Collections.emptySet());

        assertEquals(Collections.singleton("vls:menu:algorithm-management"), service.getMenuPermissions("42"));
        assertEquals(Collections.emptySet(), service.getMenuPermissions("42"));
    }

    @Test
    void retainsBuiltInAdministratorWildcard() {
        SysPermissionService service = new SysPermissionService(mock(ISysRoleService.class), mock(ISysMenuService.class));
        assertEquals(Collections.singleton("*:*:*"), service.getMenuPermissions("1"));
    }
}
