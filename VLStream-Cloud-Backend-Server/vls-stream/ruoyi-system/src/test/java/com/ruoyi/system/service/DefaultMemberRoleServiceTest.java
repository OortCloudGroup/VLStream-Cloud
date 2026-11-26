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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<Long, SysMenu> catalogue = new HashMap<>();

    @BeforeEach
    void fixture() {
        TenantContextHolder.setTenantId("fixture-tenant");
        role = new SysRole(); role.setRoleId("2001"); role.setTenantId("fixture-tenant");
        role.setRoleKey("vls_member"); role.setStatus("0"); role.setDelFlag("0"); role.setDataScope("1");
        long id = 100;
        for (String permission : DefaultMemberRoleService.BUSINESS_PERMISSIONS) addGrant(++id, permission);
        when(roles.selectOne(any())).thenReturn(role);
        when(roleMenus.selectList(any())).thenReturn(bindings);
        when(menus.selectBatchIds(any())).thenAnswer(invocation -> {
            List<SysMenu> found = new ArrayList<>();
            for (Object key : (Collection<?>) invocation.getArgument(0)) {
                SysMenu menu = catalogue.get(key);
                if (menu != null && (grants.contains(menu) || !isGranted(menu.getMenuId()))) found.add(menu);
            }
            return found;
        });
    }

    private boolean isGranted(Long id) {
        return bindings.stream().anyMatch(binding -> id.equals(binding.getMenuId()));
    }

    private SysMenu catalogueMenu(long id, String parentId, String path, String permission) {
        SysMenu menu = new SysMenu(); menu.setMenuId(id); menu.setParentId(parentId); menu.setPath(path);
        menu.setPerms(permission); menu.setStatus("0"); catalogue.put(id, menu);
        return menu;
    }

    private SysMenu addGrant(long id, String permission) {
        return addGrant(id, null, permission);
    }

    private SysMenu addGrant(long id, String parentId, String permission) {
        SysMenu menu = catalogueMenu(id, parentId, null, permission); grants.add(menu);
        SysRoleMenu binding = new SysRoleMenu(); binding.setRoleId("2001"); binding.setTenantId("fixture-tenant"); binding.setMenuId(id); bindings.add(binding);
        return menu;
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
    void systemManagementGrantsAreNeverDefaultedEvenWhenDisabled() {
        for (String permission : new String[] {"*:*:*", "system", "system:user:list", "system:role:edit", "System:menu:list"}) {
            addGrant(999, permission).setStatus("1");
            assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role), permission);
            grants.remove(grants.size() - 1); bindings.remove(bindings.size() - 1); catalogue.remove(999L);
        }
    }

    @Test
    void anythingBelowTheSystemDirectoryIsRejected() {
        catalogueMenu(900000, "0", "system", "system");
        SysMenu firmware = catalogueMenu(950, "900000", "firmware", "vls:firmware:list");
        catalogueMenu(951, "950", "#", null);
        addGrant(952, "951", "vls:compute:company-publish");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        firmware.setParentId("0");
        assertDoesNotThrow(() -> service.requireOrdinaryRole(role));
    }

    @Test
    void orphanUnderTheUpstreamSystemRootIsRejected() {
        addGrant(953, "1", "vls:compute:manage-all");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
    }

    @Test
    void everyNonSystemPageAndOperationIsAcceptedForTheDefaultTemplate() {
        catalogueMenu(920001, "0", "decision-ai", "vls:menu:group:decision-ai");
        addGrant(920010, "920001", "vls:menu:group:cloud-device-intelligence");
        addGrant(920150, "920010", "vls:menu:cloud-algorithm-config");
        addGrant(901700, "active-safety:settings:workorder");
        addGrant(901701, "901700", "workflow:model:add");
        addGrant(990001, "vls:menu:future-page");
        addGrant(990002, "vls:compute:publish");
        addGrant(990003, null);
        SysUser user = new SysUser();
        service.assignIfMissing(user);
        assertArrayEquals(new String[] {"2001"}, user.getRoleIds());
    }

    @Test
    void registeredBasicPagesAreCompatibleWithTheDefaultTemplate() {
        long id = 1000;
        for (String permission : DefaultMemberRoleService.BASIC_MENU_PERMISSIONS) addGrant(++id, permission);
        assertDoesNotThrow(() -> service.requireOrdinaryRole(role));
        SysUser user = new SysUser();
        service.assignIfMissing(user);
        assertArrayEquals(new String[] {"2001"}, user.getRoleIds());
    }

    @Test
    void emptyRoleChoiceAcceptsCloudConfigurationFromTheCatalogueMigration() {
        addGrant(920122, "vls:menu:scene-governance");
        addGrant(920010, "vls:menu:group:cloud-device-intelligence");
        addGrant(920150, "vls:menu:cloud-algorithm-config");
        SysUser user = new SysUser();
        user.setRoleIds(new String[0]);

        service.assignIfMissing(user);

        assertArrayEquals(new String[] {"2001"}, user.getRoleIds());
        assertEquals("fixture-tenant", user.getTenantId());
    }

    @Test
    void previouslyOpenSafetySettingsPageDoesNotPreventCreatingMemberUser() {
        addGrant(998, "active-safety:settings:secure");
        SysUser user = new SysUser();
        service.assignIfMissing(user);
        assertArrayEquals(new String[] {"2001"}, user.getRoleIds());
        verify(roleMenus, never()).insert(any());
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
    void danglingMenuOrForeignBindingIsRejected() {
        SysMenu removed = grants.remove(0);
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
        grants.add(removed); bindings.get(0).setTenantId("other-tenant");
        assertThrows(ServiceException.class, () -> service.requireOrdinaryRole(role));
    }

    @Test
    void administratorMayRevokeAnyOrAllDefaultBusinessGrantsWithoutBlockingNewUsers() {
        grants.clear(); bindings.clear();
        addGrant(777, "vls:menu:data-management");
        SysUser subsetUser = new SysUser();
        service.assignIfMissing(subsetUser);
        assertArrayEquals(new String[] {"2001"}, subsetUser.getRoleIds());
        grants.clear(); bindings.clear();
        SysUser emptyUser = new SysUser();
        service.assignIfMissing(emptyUser);
        assertArrayEquals(new String[] {"2001"}, emptyUser.getRoleIds());
        verify(roleMenus, never()).insert(any());
    }
}
