package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.DefaultMemberMenuRow;
import com.ruoyi.system.mapper.DefaultMemberMenuSyncMapper;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class DefaultMemberMenuSyncServiceTest {
    private final DefaultMemberMenuSyncMapper mapper = mock(DefaultMemberMenuSyncMapper.class);
    private final DefaultMemberRoleService roles = new DefaultMemberRoleService(
        mock(SysRoleMapper.class), mock(SysRoleMenuMapper.class), mock(SysMenuMapper.class));
    private final DefaultMemberMenuSyncService service = new DefaultMemberMenuSyncService(mapper, roles);
    private final List<DefaultMemberMenuRow> catalogue = new ArrayList<>();

    @BeforeEach
    void fixture() {
        SysRole member = new SysRole();
        member.setRoleId("2001");
        member.setTenantId("tenant-a");
        when(mapper.selectActiveDefaultMemberRoles("vls_member")).thenReturn(Collections.singletonList(member));
        when(mapper.selectCatalogueTenant()).thenReturn("000000");
        when(mapper.selectMenuCatalogue()).thenReturn(catalogue);
        when(mapper.insertOffer(anyString(), anyString(), anyLong())).thenReturn(1);
        when(mapper.insertGrant(anyString(), anyString(), anyLong())).thenReturn(1);
        when(mapper.deleteGrant(anyString(), anyLong())).thenReturn(1);
        menu(900000, "0", "system", "system", "0", "000000");
        menu(900100, "900000", "users", "system:user:list", "0", "000000");
    }

    private void menu(long id, String parent, String path, String perms, String status, String tenant) {
        DefaultMemberMenuRow row = new DefaultMemberMenuRow();
        row.setMenuId(id); row.setParentId(parent); row.setPath(path); row.setPerms(perms);
        row.setStatus(status); row.setTenantId(tenant);
        catalogue.add(row);
    }

    @Test
    void newMenuIsOfferedAndGrantedOnce() {
        menu(920150, "0", "/cloud-algorithm-config", "vls:menu:cloud-algorithm-config", "0", null);
        assertEquals(1, service.syncAllRoles());
        verify(mapper).insertOffer("2001", "tenant-a", 920150L);
        verify(mapper).insertGrant("2001", "tenant-a", 920150L);
    }

    @Test
    void revokedMenuIsNotGrantedAgain() {
        menu(901700, "0", "settings/work-orders", "active-safety:settings:workorder", "0", "000000");
        when(mapper.selectOfferedMenuIds("2001")).thenReturn(Collections.singletonList(901700L));
        assertEquals(0, service.syncAllRoles());
        verify(mapper, never()).insertOffer(anyString(), anyString(), anyLong());
        verify(mapper, never()).insertGrant(anyString(), anyString(), anyLong());
    }

    @Test
    void systemManagementGrantIsRemovedAndNeverOffered() {
        when(mapper.selectGrantedMenuIds("2001")).thenReturn(Arrays.asList(900000L, 900100L));
        assertEquals(2, service.syncAllRoles());
        verify(mapper).deleteGrant("2001", 900000L);
        verify(mapper).deleteGrant("2001", 900100L);
        verify(mapper, never()).insertOffer(anyString(), anyString(), anyLong());
    }

    @Test
    void disabledAndOtherTenantMenusAreSkipped() {
        menu(930001, "0", "disabled", "vls:menu:disabled", "1", null);
        menu(930002, "0", "foreign", "vls:menu:foreign", "0", "tenant-b");
        menu(930003, "0", "own", "vls:menu:own", "0", "tenant-a");
        assertEquals(1, service.syncAllRoles());
        verify(mapper).insertGrant("2001", "tenant-a", 930003L);
        verify(mapper, never()).insertOffer(anyString(), anyString(), eq(930001L));
        verify(mapper, never()).insertOffer(anyString(), anyString(), eq(930002L));
    }

    @Test
    void offerClaimedByAnotherInstanceOrExistingGrantIsNotDuplicated() {
        menu(940001, "0", "a", "vls:menu:a", "0", null);
        menu(940002, "0", "b", "vls:menu:b", "0", null);
        when(mapper.insertOffer("2001", "tenant-a", 940001L)).thenReturn(0);
        when(mapper.selectGrantedMenuIds("2001")).thenReturn(Collections.singletonList(940002L));
        assertEquals(0, service.syncAllRoles());
        verify(mapper).insertOffer("2001", "tenant-a", 940002L);
        verify(mapper, never()).insertGrant(anyString(), anyString(), anyLong());
    }

    @Test
    void failureNeverPropagatesToTheCaller() {
        when(mapper.selectMenuCatalogue()).thenThrow(new IllegalStateException("table missing"));
        assertDoesNotThrow(() -> service.syncQuietly("test"));
    }
}
