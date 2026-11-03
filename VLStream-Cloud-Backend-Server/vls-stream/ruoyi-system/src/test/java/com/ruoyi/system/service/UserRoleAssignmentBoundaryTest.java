package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class UserRoleAssignmentBoundaryTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysUserRoleViewMapper bindings = mock(SysUserRoleViewMapper.class);
    private final LoginUser actor = new LoginUser();
    private final SysUserServiceImpl service = spy(new SysUserServiceImpl(users, mock(SysDeptMapper.class),
        roles, mock(SysPostMapper.class), bindings, mock(SysUserPostMapper.class), mock(DefaultMemberRoleService.class)) {
        @Override protected LoginUser currentActor() { return actor; }
    });

    private SysRole setup() {
        actor.setUserId("9"); actor.setTenantId("a");
        SysUser target = new SysUser("20"); target.setTenantId("a");
        when(users.selectById("20")).thenReturn(target);
        doNothing().when(service).checkUserDataScope("20");
        SysRole role = new SysRole("30"); role.setTenantId("a"); role.setStatus("0"); role.setRoleKey("reader");
        when(roles.selectById("30")).thenReturn(role);
        when(roles.selectAssignedMenuIds("9")).thenReturn(Arrays.asList(1L,2L));
        when(roles.selectRoleMenuIds("30")).thenReturn(Collections.singletonList(1L));
        return role;
    }

    @Test void sameTenantSubsetCanBeAssigned() {
        setup(); service.insertUserAuth("20", new String[]{"30"});
        verify(bindings).delete(any());
        verify(bindings).insertBatch(argThat(rows -> rows.size()==1 && "a".equals(rows.iterator().next().getTenantId())));
    }

    @Test void crossTenantRoleCannotEraseCurrentAssignments() {
        setup().setTenantId("b");
        assertThrows(ServiceException.class, () -> service.insertUserAuth("20", new String[]{"30"}));
        verifyNoInteractions(bindings);
    }

    @Test void strongerRoleCannotBeAssigned() {
        setup(); when(roles.selectRoleMenuIds("30")).thenReturn(Collections.singletonList(99L));
        assertThrows(ServiceException.class, () -> service.insertUserAuth("20", new String[]{"30"}));
        verifyNoInteractions(bindings);
    }

    @Test void disabledRoleAndSuperuserCannotBeModified() {
        setup().setStatus("1");
        assertThrows(ServiceException.class, () -> service.insertUserAuth("20", new String[]{"30"}));
        assertThrows(ServiceException.class, () -> service.insertUserAuth("1", new String[0]));
        verifyNoInteractions(bindings);
    }
}
