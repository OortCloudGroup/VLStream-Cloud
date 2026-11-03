package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysUserRoleView;
import com.ruoyi.system.mapper.SysRoleDeptMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysUserRoleViewMapper;
import com.ruoyi.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class MultiUserRoleDetailTest {

    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
    private final SysUserRoleViewMapper userRoleMapper = mock(SysUserRoleViewMapper.class);
    private final SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);
    private final SysRoleServiceImpl service =
        spy(new SysRoleServiceImpl(roleMapper, roleMenuMapper, userRoleMapper, roleDeptMapper));

    @Test
    void roleDetailExistsWithoutAssignmentsAndIncludesMetadata() {
        SysRole view = new SysRole();
        view.setRoleId("42");
        view.setRoleName("多用户验证角色");
        view.setRoleKey("custom_role");
        when(roleMapper.selectById("42")).thenReturn(view);

        SysRole result = service.selectRoleDefinitionById("42");

        assertEquals("多用户验证角色", result.getRoleName());
        assertEquals("custom_role", result.getRoleKey());
        verify(roleMapper).selectById("42");
        verify(userRoleMapper, never()).selectById(anyString());
    }

    @Test
    void metadataUpdatePreservesMenuGrantsTenantAndDataScope() {
        SysRole stored = new SysRole("42");
        stored.setRoleKey("custom_role");
        stored.setTenantId("tenant-a");
        stored.setDataScope("5");
        when(roleMapper.selectById("42")).thenReturn(stored);
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);
        doNothing().when(service).checkRoleDataScope("42");
        SysRole update = new SysRole("42");
        update.setRoleKey("renamed");
        update.setTenantId("tenant-b");
        update.setDataScope("1");
        update.setMenuIds(new Long[0]);
        assertEquals(1, service.updateRole(update));
        assertEquals("tenant-a", update.getTenantId());
        assertEquals("5", update.getDataScope());
        verify(roleMenuMapper, never()).delete(any());
        verify(roleMenuMapper, never()).insertBatch(any());
    }

    @Test
    void rejectsRenamingCustomRoleIntoAdministrator() {
        SysRole stored = new SysRole("42");
        stored.setRoleKey("custom_role");
        when(roleMapper.selectById("42")).thenReturn(stored);
        doNothing().when(service).checkRoleDataScope("42");
        SysRole update = new SysRole("42");
        update.setRoleKey("tenant_admin");
        assertThrows(ServiceException.class, () -> service.updateRole(update));
        verify(roleMapper, never()).updateById(any(SysRole.class));
    }

    @Test
    void assignedRoleReturnsBusinessErrorWhenItHasMultipleUsers() {
        SysRole role = new SysRole("42");
        role.setRoleKey("custom_role");
        role.setRoleName("多用户验证角色");
        when(roleMapper.selectById("42")).thenReturn(role);
        when(userRoleMapper.selectCount(any())).thenReturn(2L);
        doNothing().when(service).checkRoleDataScope(anyString());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.deleteRoleByIds(new String[] { "42" }));

        assertEquals("多用户验证角色已分配,不能删除", exception.getMessage());
        assertEquals(409, exception.getCode());
        verify(userRoleMapper, never()).selectById(anyString());
    }

    @Test
    void zeroUserRoleEntersRealDeleteFlow() {
        SysRole role = new SysRole("42");
        role.setRoleKey("custom_role");
        role.setRoleName("多用户验证角色");
        when(roleMapper.selectById("42")).thenReturn(role);
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(roleMenuMapper.delete(any())).thenReturn(1);
        when(roleDeptMapper.delete(any())).thenReturn(1);
        when(roleMapper.deleteBatchIds(any())).thenReturn(1);
        doNothing().when(service).checkRoleDataScope(anyString());

        assertEquals(1, service.deleteRoleByIds(new String[] { "42" }));
        verify(userRoleMapper, never()).selectById(anyString());
    }
}
