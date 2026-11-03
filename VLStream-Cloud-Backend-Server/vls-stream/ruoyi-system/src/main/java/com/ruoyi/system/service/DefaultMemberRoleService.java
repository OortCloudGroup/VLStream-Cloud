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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    // Explicitly match the versioned basic console catalogue. Do not
    // accept arbitrary vls:menu:* strings: future pages may manage credentials.
    public static final Set<String> BASIC_MENU_PERMISSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "vls:menu:group:video-aggregation", "vls:menu:group:decision-ai", "vls:menu:group:algorithm-warehouse",
        "vls:menu:group:ai-computing", "vls:menu:group:device-management", "vls:menu:group:gb-protocol",
        "vls:menu:group:event-management", "vls:menu:group:edge-intelligence", "vls:menu:group:cloud-intelligence",
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

    /** Validate all stored grants, including disabled menu rows; no fallback to an administrator template. */
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
        Set<String> permissions = granted.stream().map(SysMenu::getPerms)
            .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Set<String> permitted = new HashSet<>(BUSINESS_PERMISSIONS);
        permitted.addAll(BASIC_MENU_PERMISSIONS);
        permitted.addAll(COMPATIBLE_PAGE_PERMISSIONS);
        if (granted.size() != ids.size() || granted.stream().anyMatch(menu -> StringUtils.isBlank(menu.getPerms()))
            || !permitted.containsAll(permissions)) {
            throw new ServiceException("默认普通用户角色包含无效菜单或未经允许的管理权限，请检查角色授权");
        }
    }
}
