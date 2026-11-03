package com.ruoyi.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.RoleMenuGrantUpdate;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class RoleMenuGrantServiceTest {
    private SysRoleMapper roles;
    private SysRoleMenuMapper grants;
    private ISysRoleService roleService;
    private ISysMenuService menuService;
    private RoleMenuGrantService service;
    private SysRole role;

    @BeforeEach
    void setup() {
        roles = mock(SysRoleMapper.class);
        grants = mock(SysRoleMenuMapper.class);
        roleService = mock(ISysRoleService.class);
        menuService = mock(ISysMenuService.class);
        service = spy(new RoleMenuGrantService(roles, grants, roleService, menuService));
        LoginUser login = new LoginUser();
        login.setTenantId("tenant-a");
        doReturn(login).when(service).currentLoginUser();
        doReturn("operator").when(service).currentUserId();
        role = new SysRole("9");
        role.setTenantId("tenant-a");
        when(roles.selectOne(any(Wrapper.class))).thenReturn(role);
        when(menuService.selectMenuList("operator")).thenReturn(Arrays.asList(menu(1), menu(2)));
        when(menuService.buildMenuTreeSelect(anyList())).thenReturn(Arrays.asList(tree(1), tree(2)));
        when(grants.selectGrantableMenuIds("operator", "tenant-a")).thenReturn(Arrays.asList(1L, 2L));
    }

    @Test
    void updatesOnlyCallerVisibleGrantsAndPreservesOtherTenantAndHiddenRows() {
        when(grants.selectAllForVerifiedRole("9")).thenReturn(Arrays.asList(
            grant(1, "tenant-a"), grant(777, "tenant-b"), grant(999, "tenant-a")));
        service.updateMenuIds("9", request(Arrays.asList("2"), Arrays.asList("1", "777", "999")));
        verify(grants, times(1)).delete(any(Wrapper.class));
        ArgumentCaptor<SysRoleMenu> inserted = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(grants).insert(inserted.capture());
        assertEquals(2L, inserted.getValue().getMenuId());
        assertEquals("tenant-a", inserted.getValue().getTenantId());
        assertEquals("9", inserted.getValue().getRoleId());
        verify(roleService, never()).cleanOnlineUserByRole(anyString());
    }

    @Test
    void rejectsStaleSnapshotAndOutOfScopeIdsWithoutWrites() {
        when(grants.selectAllForVerifiedRole("9")).thenReturn(Collections.singletonList(grant(1, "tenant-a")));
        assertThrows(ServiceException.class, () -> service.updateMenuIds("9", request(Collections.emptyList(), Collections.emptyList())));
        assertThrows(ServiceException.class, () -> service.updateMenuIds("9", request(Collections.singletonList("777"), Collections.singletonList("1"))));
        verify(grants, never()).insert(any(SysRoleMenu.class));
        verify(grants, never()).delete(any(Wrapper.class));
    }

    @Test
    void rejectsCrossTenantRoleBeforeReadingGrants() {
        role.setTenantId("tenant-b");
        assertThrows(ServiceException.class, () -> service.getMenuIds("9"));
        verify(grants, never()).selectAllForVerifiedRole(anyString());
    }

    @Test
    void rejectsMenuOnlyVisibleThroughAnInactiveRole() {
        when(grants.selectAllForVerifiedRole("9")).thenReturn(Collections.emptyList());
        when(grants.selectGrantableMenuIds("operator", "tenant-a")).thenReturn(Collections.singletonList(1L));
        assertThrows(ServiceException.class,
            () -> service.updateMenuIds("9", request(Collections.singletonList("2"), Collections.emptyList())));
        verify(grants, never()).insert(any(SysRoleMenu.class));
    }

    @Test
    void grantableIdsExcludeDisabledMenuAndInactiveAssignments() {
        SysMenu disabled = menu(2);
        disabled.setStatus("1");
        when(menuService.selectMenuList("operator")).thenReturn(Arrays.asList(menu(1), disabled, menu(3)));
        when(grants.selectGrantableMenuIds("operator", "tenant-a")).thenReturn(Arrays.asList(1L, 2L));
        assertEquals(Collections.singletonList("1"), service.getGrantableMenuIds("9"));
    }

    @Test
    void preservesAndCannotAddAnAssignedMenuMissingFromTheVisibleTree() {
        when(menuService.selectMenuList("operator")).thenReturn(Arrays.asList(menu(1), menu(3)));
        when(menuService.buildMenuTreeSelect(anyList())).thenReturn(Collections.singletonList(tree(1)));
        when(grants.selectGrantableMenuIds("operator", "tenant-a")).thenReturn(Arrays.asList(1L, 3L));
        when(grants.selectAllForVerifiedRole("9")).thenReturn(Collections.singletonList(grant(3, "tenant-a")));
        service.updateMenuIds("9", request(Collections.emptyList(), Collections.singletonList("3")));
        verify(grants, never()).delete(any(Wrapper.class));
        verify(grants, never()).insert(any(SysRoleMenu.class));
        assertThrows(ServiceException.class,
            () -> service.updateMenuIds("9", request(Collections.singletonList("3"), Collections.singletonList("3"))));
    }

    @Test
    void returnsLargeIdsAsExactStrings() {
        when(grants.selectAllForVerifiedRole("9")).thenReturn(Collections.singletonList(
            grant(1900000000000000700L, "tenant-a")));
        assertEquals(Collections.singletonList("1900000000000000700"), service.getMenuIds("9"));
    }

    @Test
    void protectsSuperAdministratorAndRejectsMalformedIds() {
        role.setRoleId("1");
        assertThrows(ServiceException.class, () -> service.updateMenuIds("1", request(Collections.emptyList(), Collections.emptyList())));
        verify(grants, never()).selectAllForVerifiedRole(anyString());
        assertThrows(ServiceException.class, () -> service.getMenuIds("9 OR 1=1"));
    }

    @Test
    void permitsActualSuperuserToEditProtectedRoleMenuBindingsOnly() {
        role.setRoleId("1");
        doReturn("1").when(service).currentUserId();
        when(grants.selectAllForVerifiedRole("1")).thenReturn(Collections.singletonList(grant(1, "tenant-a")));
        when(menuService.selectMenuList("1")).thenReturn(Arrays.asList(menu(1), menu(2)));
        service.updateMenuIds("1", request(Collections.singletonList("2"), Collections.singletonList("1")));
        verify(grants).delete(any(Wrapper.class));
        verify(grants).insert(any(SysRoleMenu.class));
        verify(roles, never()).updateById(any(SysRole.class));
    }

    @Test
    void superuserCannotEditProtectedRoleInAnotherTenant() {
        role.setRoleId("1");
        role.setTenantId("tenant-b");
        doReturn("1").when(service).currentUserId();
        assertThrows(ServiceException.class,
            () -> service.updateMenuIds("1", request(Collections.emptyList(), Collections.emptyList())));
        verify(grants, never()).selectAllForVerifiedRole(anyString());
    }

    private static SysMenu menu(long id) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(id);
        menu.setStatus("0");
        return menu;
    }

    private static Tree<String> tree(long id) {
        Tree<String> tree = new Tree<>();
        tree.setId(String.valueOf(id));
        return tree;
    }

    private static SysRoleMenu grant(long id, String tenant) {
        SysRoleMenu grant = new SysRoleMenu();
        grant.setRoleId("9");
        grant.setTenantId(tenant);
        grant.setMenuId(id);
        return grant;
    }

    private static RoleMenuGrantUpdate request(java.util.List<String> ids, java.util.List<String> expected) {
        RoleMenuGrantUpdate request = new RoleMenuGrantUpdate();
        request.setMenuIds(ids);
        request.setExpectedMenuIds(expected);
        return request;
    }
}
