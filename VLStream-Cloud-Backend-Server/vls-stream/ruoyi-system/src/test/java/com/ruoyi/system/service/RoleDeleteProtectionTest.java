package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysRoleDeptMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysUserRoleViewMapper;
import com.ruoyi.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class RoleDeleteProtectionTest {

    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
    private final SysUserRoleViewMapper userRoleMapper = mock(SysUserRoleViewMapper.class);
    private final SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);
    private final SysRoleServiceImpl service =
        new SysRoleServiceImpl(roleMapper, roleMenuMapper, userRoleMapper, roleDeptMapper);

    @Test
    void blocksSuperAdministratorRole() {
        SysRole role = new SysRole("1");
        role.setRoleKey("admin");
        when(roleMapper.selectById("1")).thenReturn(role);
        assertThrows(ServiceException.class, () -> service.deleteRoleByIds(new String[] { "1" }));
    }

    @Test
    void blocksBuiltInRoleKeys() {
        for (String key : new String[] { "admin", "tenant_admin", "vls_member" }) {
            SysRole role = new SysRole("42");
            role.setRoleKey(key);
            when(roleMapper.selectById("42")).thenReturn(role);
            ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleteRoleByIds(new String[] { "42" }));
            assertEquals("初始化角色不允许删除", exception.getMessage());
        }
    }

    @Test
    void blocksHistoricalDefaultMemberRoleId() {
        SysRole role = new SysRole("1900000000000004001");
        role.setRoleKey("renamed");
        when(roleMapper.selectById("1900000000000004001")).thenReturn(role);
        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.deleteRoleByIds(new String[] { "1900000000000004001" }));
        assertEquals("初始化角色不允许删除", exception.getMessage());
    }
}
