package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
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
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysUserRoleViewMapper userRoles = mock(SysUserRoleViewMapper.class);
    private final DefaultMemberRoleService defaults = mock(DefaultMemberRoleService.class);
    private final SysUserServiceImpl service = new SysUserServiceImpl(users, mock(SysDeptMapper.class),
        roles, mock(SysPostMapper.class), userRoles, mock(SysUserPostMapper.class), defaults);

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
    void verifiedPlatformMemberDoesNotNeedAnExistingLocalSession() {
        SysUser user = user();
        user.setPlatformUserId("platform-user");
        user.setTenantId("verified-tenant");
        user.setRoleIds(new String[] {"42"});
        SysRole memberRole = new SysRole();
        memberRole.setRoleId("42");
        when(roles.selectById("42")).thenReturn(memberRole);
        TenantContextHolder.setTenantId("verified-tenant");
        try {
            assertEquals(1, service.insertVerifiedPlatformUser(user));
            verify(defaults).requireOrdinaryRole(memberRole);
            verify(userRoles).insertBatch(any(Collection.class));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void verifiedPlatformMemberCannotBeInsertedIntoAnotherTenant() {
        SysUser user = user();
        user.setPlatformUserId("platform-user");
        user.setTenantId("other-tenant");
        user.setRoleIds(new String[] {"42"});
        TenantContextHolder.setTenantId("verified-tenant");
        try {
            assertThrows(ServiceException.class, () -> service.insertVerifiedPlatformUser(user));
            verifyNoInteractions(roles);
            verify(users, never()).insert(user);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void failedRegistrationDoesNotCreateRoleBinding() {
        SysUser user = user(); when(users.insert(user)).thenReturn(0);
        assertFalse(service.registerUser(user));
        verify(userRoles, never()).insertBatch(any(Collection.class));
    }

    @Test
    void existingUserEditDoesNotApplyNewDefault() {
        SysUser user = new SysUser(); user.setUserId("fixture-existing");
        service.updateUser(user);
        verifyNoInteractions(defaults);
        verify(userRoles, never()).delete(any());
        verify(userRoles, never()).insertBatch(any(Collection.class));
    }

    @Test
    void failedCreationRoleWriteDoesNotReportSuccessfulAccountCreation() {
        SysUser user = user(); when(userRoles.insertBatch(any(Collection.class))).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.insertUser(user));
        assertThrows(ServiceException.class, () -> service.registerUser(user));
    }
}
