package com.ruoyi.system.service;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DefaultMemberMenuRow;
import com.ruoyi.system.mapper.DefaultMemberMenuSyncMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps every tenant's default member role (vls_member) at "all pages and operations
 * except system management". Each active non-system menu is offered to a role once;
 * the offer is recorded so a later revocation by an administrator is respected while
 * menus added afterwards (by migration or the menu page) are still granted.
 * System management grants are always removed, because they would block member creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMemberMenuSyncService {
    private final DefaultMemberMenuSyncMapper syncMapper;
    private final DefaultMemberRoleService defaultMemberRoles;

    /** Runs after Flyway has applied migrations, so menus added by a release are picked up. */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        syncQuietly("startup");
    }

    /** Never lets synchronization failure break the caller (startup or a menu edit). */
    public void syncQuietly(String reason) {
        try {
            int changed = syncAllRoles();
            if (changed > 0) {
                log.info("默认普通成员角色菜单已同步（{}）：变更 {} 条授权", reason, changed);
            }
        } catch (RuntimeException exception) {
            log.warn("默认普通成员角色菜单同步失败（{}），将在下次启动或菜单变更时重试", reason, exception);
        }
    }

    public synchronized int syncAllRoles() {
        List<DefaultMemberMenuRow> rows = syncMapper.selectMenuCatalogue();
        List<SysMenu> catalogue = new ArrayList<>();
        for (DefaultMemberMenuRow row : rows) {
            catalogue.add(row.toMenu());
        }
        Set<Long> systemManagement = defaultMemberRoles.findSystemManagementMenuIds(catalogue);
        String catalogueTenant = syncMapper.selectCatalogueTenant();
        int changed = 0;
        for (SysRole role : syncMapper.selectActiveDefaultMemberRoles(UserConstants.DEFAULT_MEMBER_ROLE_KEY)) {
            changed += syncRole(role, rows, systemManagement, catalogueTenant);
        }
        return changed;
    }

    /** A tenant role copied from the template inherits the template's offers (and revocations). */
    public void inheritOffers(String templateRoleId, String roleId, String tenantId) {
        syncMapper.copyOffers(templateRoleId, roleId, tenantId);
    }

    private int syncRole(SysRole role, List<DefaultMemberMenuRow> rows, Set<Long> systemManagement, String catalogueTenant) {
        String roleId = role.getRoleId();
        String tenantId = role.getTenantId();
        Set<Long> granted = new HashSet<>(syncMapper.selectGrantedMenuIds(roleId));
        Set<Long> offered = new HashSet<>(syncMapper.selectOfferedMenuIds(roleId));
        int changed = 0;
        for (DefaultMemberMenuRow row : rows) {
            Long menuId = row.getMenuId();
            if (menuId == null) {
                continue;
            }
            if (systemManagement.contains(menuId)) {
                if (granted.contains(menuId)) {
                    changed += syncMapper.deleteGrant(roleId, menuId);
                }
                continue;
            }
            if (offered.contains(menuId) || !"0".equals(row.getStatus()) || !visibleToTenant(row, tenantId, catalogueTenant)) {
                continue;
            }
            // Recording the offer first makes concurrent instances grant at most once.
            if (syncMapper.insertOffer(roleId, tenantId, menuId) == 1 && !granted.contains(menuId)) {
                changed += syncMapper.insertGrant(roleId, tenantId, menuId);
            }
        }
        return changed;
    }

    /** Shared menus and the catalogue tenant's menus are visible everywhere; other tenants' own menus are not. */
    private static boolean visibleToTenant(DefaultMemberMenuRow row, String tenantId, String catalogueTenant) {
        String owner = StringUtils.trimToEmpty(row.getTenantId());
        return owner.isEmpty() || owner.equals(tenantId) || Objects.equals(owner, catalogueTenant);
    }
}
