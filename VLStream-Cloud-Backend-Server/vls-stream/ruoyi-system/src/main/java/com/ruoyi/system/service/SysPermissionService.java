/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LivePermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * user Process
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SysPermissionService implements LivePermissionProvider {

    private final ISysRoleService roleService;
    private final ISysMenuService menuService;

    @Override
    public Set<String> getMenuPermissions(String userId) {
        return getMenuPermission(new SysUser(userId));
    }

    @Override
    public Set<String> getRolePermissions(String userId) {
        return getRolePermission(new SysUser(userId));
    }

    /**
     * Get roledata
     *
     * @param user userinfo
     * @return role info
     */
    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new HashSet<>();
        // administrator all
        if (user.isAdmin()) {
            roles.add("admin");
        } else {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * Get menudata
     *
     * @param user userinfo
     * @return menu info
     */
    public Set<String> getMenuPermission(SysUser user) {
        Set<String> perms = new HashSet<>();
        // administrator all
        if (user.isAdmin() || getRolePermission(user).contains("admin")) {
            perms.add("*:*:*");
        } else {
            perms.addAll(getExplicitMenuPermission(user));
        }
        return perms;
    }

    /** Reads actual enabled menu assignments, including for administrators, without expanding wildcards. */
    public Set<String> getExplicitMenuPermission(SysUser user) {
        return new HashSet<>(menuService.selectMenuPermsByUserId(user.getUserId()));
    }
}
