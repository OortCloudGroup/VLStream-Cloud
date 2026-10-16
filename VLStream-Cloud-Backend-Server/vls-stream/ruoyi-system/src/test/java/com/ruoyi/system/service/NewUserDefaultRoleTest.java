package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysUserRoleView;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class NewUserDefaultRoleTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SysUserRoleViewMapper userRoles = mock(SysUserRoleViewMapper.class);
    private final DefaultMemberRoleService defaults = mock(DefaultMemberRoleService.class);
    private final SysUserServiceImpl service = new SysUserServiceImpl(users, mock(SysDeptMapper.class),
        mock(SysRoleMapper.class), mock(SysPostMapper.class), userRoles, mock(SysUserPostMapper.class), defaults);

    private SysUser user() {
        SysUser user = new SysUser(); user.setUserId("fixture-new-user"); user.setUserName("fixture");
        doAnswer(call -> { ((SysUser)call.getArgument(0)).setRoleIds(new String[] {"member-fixture-role"}); return null; })
            .when(defaults).assignIfMissing(user);
        when(users.insert(user)).thenReturn(1);
        when(userRoles.insertBatch(any(Collection.class))).thenReturn(true);
        return user;
    }

    @Test
    void administrationAndImportCreationBindDefaultBeforeInsert() {
        SysUser user = user(); assertEquals(1, service.insertUser(user));
        org.mockito.InOrder order = inOrder(defaults, users, userRoles);
        order.verify(defaults).assignIfMissing(user); order.verify(users).insert(user);
        ArgumentCaptor<Collection<SysUserRoleView>> rows = ArgumentCaptor.forClass(Collection.class);
        verify(userRoles).insertBatch(rows.capture());
        assertEquals("member-fixture-role", rows.getValue().iterator().next().getRoleId());
    }

    @Test
    void registrationAlsoPersistsOrdinaryRole() {
        SysUser user = user(); assertTrue(service.registerUser(user));
        verify(defaults).assignIfMissing(user);
        verify(userRoles).insertBatch(any(Collection.class));
    }

    @Test
    void failedRegistrationDoesNotCreateRoleBinding() {
        SysUser user = user(); when(users.insert(user)).thenReturn(0);
        assertFalse(service.registerUser(user));
        verify(userRoles, never()).insertBatch(any(Collection.class));
    }

    @Test
    void existingUserEditDoesNotApplyNewDefault() {
        SysUser user = new SysUser(); user.setUserId("fixture-existing"); user.setRoleIds(new String[] {"existing-admin-role"});
        service.updateUser(user);
        verifyNoInteractions(defaults);
        assertArrayEquals(new String[] {"existing-admin-role"}, user.getRoleIds());
    }

    @Test
    void failedCreationRoleWriteDoesNotReportSuccessfulAccountCreation() {
        SysUser user = user(); when(userRoles.insertBatch(any(Collection.class))).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.insertUser(user));
        assertThrows(ServiceException.class, () -> service.registerUser(user));
    }
}
