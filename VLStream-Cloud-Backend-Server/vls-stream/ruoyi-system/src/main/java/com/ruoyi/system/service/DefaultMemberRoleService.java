package com.ruoyi.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Defaults apply only to creation; existing user and administrator assignments are never rewritten. */
@Service
@RequiredArgsConstructor
public class DefaultMemberRoleService {
    public static final Set<String> BUSINESS_PERMISSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "active-safety:list", "active-safety:events:list", "active-safety:workorder:my", "active-safety:workorder:pending",
        "active-safety:workorder:completed", "active-safety:workorder:claimable")));
    // This legacy settings page was previously available after application/group authorization.
    public static final Set<String> COMPATIBLE_PAGE_PERMISSIONS = Collections.singleton("active-safety:settings:secure");
    // Built-in basic console catalogue maintained by migrations (protected from menu edits).
    // Default member eligibility is decided by findSystemManagementMenuIds, not by this list.
    public static final Set<String> BASIC_MENU_PERMISSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "vls:menu:group:video-aggregation", "vls:menu:group:decision-ai", "vls:menu:group:algorithm-warehouse",
        "vls:menu:group:ai-computing", "vls:menu:group:device-management", "vls:menu:group:gb-protocol",
        "vls:menu:group:event-management", "vls:menu:group:edge-intelligence", "vls:menu:group:cloud-intelligence",
        "vls:menu:group:cloud-device-intelligence", "vls:menu:cloud-algorithm-config",
        "vls:menu:group:algorithm-training-platform", "vls:menu:video-square", "vls:menu:video-playback",
        "vls:menu:intelligent-analysis-result", "vls:menu:vlstream:device", "vls:menu:isup:isupDevice",
        "vls:menu:ehome:device", "vls:menu:rtsp:rtspDevice", "vls:menu:onvif:cameraManage",
        "vls:menu:gbmanger:device", "vls:menu:gbmanger:cloudRecord", "vls:menu:gbmanger:streamPush",
        "vls:menu:gbmanger:streamProxy", "vls:menu:gbmanger:platform", "vls:menu:gbmanger:recordPlan",
        "vls:menu:gbmanger:administrativeGrouping", "vls:menu:gbmanger:thoroughfare", "vls:menu:gbmanger:alarm",
        "vls:menu:gbmanger:node", "vls:menu:gbmanger:wvpLive", "vls:menu:custom:device",
        "vls:menu:llm-review-tasks", "vls:menu:algorithm-arrangement", "vls:menu:scene-governance",
        "vls:menu:algorithm-task", "vls:menu:task-status", "vls:menu:intelligent-analysis-request",
        "vls:menu:algorithm-management", "vls:menu:llm-provider-management", "vls:menu:data-management", "vls:menu:smart-annotation",
        "vls:menu:algorithm-standard", "vls:menu:algorithm-training", "vls:menu:algorithm-model",
        "vls:menu:container-instances", "vls:menu:compute-market", "vls:menu:compute-management",
        "vls:menu:legacy-device-management")));

    private final SysRoleMapper roles;
    private final SysRoleMenuMapper roleMenus;
    private final SysMenuMapper menus;

    public static boolean isAdministratorKey(String key) {
        return StringUtils.isBlank(key) || "admin".equalsIgnoreCase(key.trim()) || "tenant_admin".equalsIgnoreCase(key.trim());
    }

    /** Upstream RuoYi system directory id; the local seed uses a root directory with path "system". */
    private static final String LEGACY_SYSTEM_ROOT_ID = "1";
    private static final int MAX_MENU_DEPTH = 16;

    /**
     * The default member receives every page and operation except the system management module:
     * the root directory whose path is "system" and everything below it, plus any system:* or
     * wildcard permission. Used both when editing the template and when assigning it.
     */
    public Set<Long> findSystemManagementMenuIds(Collection<SysMenu> candidates) {
        Map<Long, SysMenu> known = new HashMap<>();
        for (SysMenu menu : candidates) {
            if (menu != null && menu.getMenuId() != null) {
                known.put(menu.getMenuId(), menu);
            }
        }
        for (int level = 0; level < MAX_MENU_DEPTH; level++) {
            Set<Long> missingParents = new HashSet<>();
            for (SysMenu menu : known.values()) {
                Long parent = parentId(menu);
                if (parent != null && !known.containsKey(parent)) {
                    missingParents.add(parent);
                }
            }
            missingParents.removeAll(known.keySet());
            if (missingParents.isEmpty()) {
                break;
            }
            List<SysMenu> parents = menus.selectBatchIds(missingParents);
            if (parents == null || parents.isEmpty()) {
                break;
            }
            boolean added = false;
            for (SysMenu parent : parents) {
                if (parent != null && parent.getMenuId() != null && known.putIfAbsent(parent.getMenuId(), parent) == null) {
                    added = true;
                }
            }
            if (!added) {
                break;
            }
        }
        Set<Long> result = new HashSet<>();
        for (SysMenu menu : candidates) {
            if (menu != null && menu.getMenuId() != null && isUnderSystemManagement(menu, known)) {
                result.add(menu.getMenuId());
            }
        }
        return result;
    }

    private static boolean isUnderSystemManagement(SysMenu menu, Map<Long, SysMenu> known) {
        Set<Long> visited = new HashSet<>();
        SysMenu current = menu;
        while (current != null && visited.add(current.getMenuId())) {
            if (isSystemManagementNode(current)) {
                return true;
            }
            String parent = StringUtils.trimToEmpty(current.getParentId());
            // An orphan left under the upstream system root (id 1) still belongs to system management.
            if (LEGACY_SYSTEM_ROOT_ID.equals(parent) && !known.containsKey(1L)) {
                return true;
            }
            Long parentId = parentId(current);
            current = parentId == null ? null : known.get(parentId);
        }
        return false;
    }

    private static boolean isSystemManagementNode(SysMenu menu) {
        String permission = StringUtils.trimToEmpty(menu.getPerms());
        String parent = StringUtils.trimToEmpty(menu.getParentId());
        return Long.valueOf(1L).equals(menu.getMenuId())
            || "*:*:*".equals(permission) || "system".equalsIgnoreCase(permission)
            || permission.toLowerCase(Locale.ROOT).startsWith("system:")
            || ("0".equals(parent) && "system".equalsIgnoreCase(StringUtils.trimToEmpty(menu.getPath())));
    }

    private static Long parentId(SysMenu menu) {
        String parent = StringUtils.trimToEmpty(menu.getParentId());
        if (!parent.matches("[1-9][0-9]{0,18}")) {
            return null;
        }
        try {
            return Long.valueOf(parent);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void assignIfMissing(SysUser user) {
        if (user.getRoleIds() != null && Arrays.stream(user.getRoleIds()).anyMatch(StringUtils::isNotBlank)) {
            return; // A separately authorized explicit choice retains its existing semantics.
        }
        String tenant = TenantContextHolder.getTenantId();
        if (StringUtils.isBlank(tenant) || (StringUtils.isNotBlank(user.getTenantId()) && !tenant.equals(user.getTenantId()))) {
            throw new ServiceException("新增用户缺少可信租户上下文或租户不一致");
        }
        SysRole role = roles.selectOne(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getTenantId, tenant).eq(SysRole::getRoleKey, UserConstants.DEFAULT_MEMBER_ROLE_KEY));
        requireOrdinaryRole(role);
        user.setTenantId(tenant);
        user.setRoleIds(new String[] {role.getRoleId()});
    }

    /** Validate all stored grants, including disabled menu rows: anything except system management is allowed. */
    public void requireOrdinaryRole(SysRole role) {
        if (role == null || "1".equals(role.getRoleId()) || isAdministratorKey(role.getRoleKey()) || !"0".equals(role.getStatus())
            || !"0".equals(role.getDelFlag()) || StringUtils.isBlank(TenantContextHolder.getTenantId())
            || !Objects.equals(TenantContextHolder.getTenantId(), role.getTenantId())) {
            throw new ServiceException("普通用户角色尚未初始化或配置不安全，请检查普通成员迁移与默认角色配置");
        }
        List<SysRoleMenu> bindings = roleMenus.selectList(new LambdaQueryWrapper<SysRoleMenu>()
            .eq(SysRoleMenu::getRoleId, role.getRoleId()));
        if (bindings.stream().anyMatch(binding -> !Objects.equals(role.getTenantId(), binding.getTenantId()))) {
            throw new ServiceException("默认普通用户菜单映射的租户不一致");
        }
        Set<Long> ids = bindings.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        List<SysMenu> granted = ids.isEmpty() ? Collections.emptyList() : menus.selectBatchIds(ids);
        if (granted.size() != ids.size()) {
            throw new ServiceException("默认普通用户角色包含已删除的菜单，请检查角色授权");
        }
        if (!findSystemManagementMenuIds(granted).isEmpty()) {
            throw new ServiceException("默认普通用户角色包含系统管理模块权限，请在角色授权中移除系统管理菜单");
        }
    }
}
