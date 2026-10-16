package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class DefaultMemberRoleServiceTest {
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysRoleMenuMapper roleMenus = mock(SysRoleMenuMapper.class);
    private final SysMenuMapper menus = mock(SysMenuMapper.class);
    private final DefaultMemberRoleService service = new DefaultMemberRoleService(roles, roleMenus, menus);
    private SysRole role;
    private final List<SysMenu> grants = new ArrayList<>();
    private final List<SysRoleMenu> bindings = new ArrayList<>();

    @BeforeEach
    void fixture() {
        TenantContextHolder.setTenantId("fixture-tenant");
        role = new SysRole(); role.setRoleId("2001"); role.setTenantId("fixture-tenant");
        role.setRoleKey("vls_member"); role.setStatus("0"); role.setDelFlag("0"); role.setDataScope("1");
        long id = 100;
        for (String permission : DefaultMemberRoleService.BUSINESS_PERMISSIONS) addGrant(++id, permission);
        when(roles.selectOne(any())).thenReturn(role);
        when(roleMenus.selectList(any())).thenReturn(bindings);
        when(menus.selectBatchIds(any())).thenReturn(grants);
    }

    private void addGrant(long id, String permission) {
        SysMenu menu = new SysMenu(); menu.setMenuId(id); menu.setPerms(permission); menu.setStatus("0"); grants.add(menu);
        SysRoleMenu binding = new SysRoleMenu(); binding.setRoleId("2001"); binding.setTenantId("fixture-tenant"); binding.setMenuId(id); bindings.add(binding);
    }

    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test
    void emptyOrBlankRoleChoiceGetsOrdinaryRoleInTrustedTenant() {
        SysUser user = new SysUser(); user.setRoleIds(new String[] {"", null});
        service.assignIfMissing(user);
        assertArrayEquals(new String[] {"2001"}, user.getRoleIds());
        assertEquals("fixture-tenant", user.getTenantId());
        verify(roles, never()).insert(any());
        verify(roleMenus, never()).insert(any());
    }

    @Test
    void explicitCreationChoiceIsNotOverwrittenOrExpanded() {
        TenantContextHolder.clear();
        SysUser user = new SysUser(); user.setRoleIds(new String[] {"explicit-existing-role"});
        service.assignIfMissing(user);
        assertArrayEquals(new String[] {"explicit-existing-role"}, user.getRoleIds());
        verifyNoInteractions(roles, roleMenus, menus);
    }

    @Test
    void missingOrSpoofedTenantIsRejectedBeforeRoleLookup() {
        SysUser user = new SysUser(); user.setTenantId("other-tenant");
        assertThrows(ServiceException.class, () -> service.assignIfMissing(user));
        TenantContextHolder.clear(); user.setTenantId(null);
        assertThrows(ServiceException.class, () -> service.assignIfMissing(user));
        verifyNoInteractions(roles, roleMenus, menus);
    }

    @Test
    void missingTemplateHasNoAdminFallback() {
        when(roles.selectOne(any())).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.assignIfMissing(new SysUser()));
        verifyNoInteractions(roleMenus, menus);
    }

    @Test
    void wildcardSystemAndComputeGrantsAreNeverDefaultedEvenWhenDisabled() {
        for (String permission : new String[] {"*:*:*", "system:user:list", "active-safety:settings:secure",
            "active-safety:settings:workorder", "vls:compute:configure",
            "vls:compute:publish", "vls:compute:manage-all", "vls:compute:company-publish"}) {
            addGrant(999, permission); grants.get(grants.size() - 1).setStatus("1");
            assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
            grants.remove(grants.size() - 1); bindings.remove(bindings.size() - 1);
        }
    }

    @Test
    void administratorRolesAndInactiveOrForeignRolesAreRejected() {
        for (String key : new String[] {"admin", "tenant_admin", " Admin ", "TENANT_ADMIN"}) {
            role.setRoleKey(key); assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        }
        role.setRoleKey("vls_member"); role.setRoleId("1");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        role.setRoleId("2001"); role.setStatus("1");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        role.setStatus("0"); role.setDelFlag("2");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        role.setDelFlag("0"); role.setTenantId("other-tenant");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
    }

    @Test
    void missingBusinessMenuOrForeignBindingDoesNotSilentlyReduceCapabilities() {
        SysMenu removed = grants.remove(0);
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        grants.add(removed); bindings.get(0).setTenantId("other-tenant");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
    }
}
