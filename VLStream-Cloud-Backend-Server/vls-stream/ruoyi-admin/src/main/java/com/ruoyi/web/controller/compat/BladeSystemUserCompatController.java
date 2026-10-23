/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.system.service.SysPermissionService;

import com.ruoyi.vlstream.test.compat.BladeResult;
import com.ruoyi.vlstream.test.compat.BladeUserInfoBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/blade-system/user")
public class BladeSystemUserCompatController {
    private static final Set<String> WVP_SHARING_PERMISSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "vlstream:device:share:manage", "vlstream:device:share:approve"
    )));

    private final BladeTokenUserStore tokenUserStore;
    private final SysPermissionService permissionService;
    private final BladeUserInfoBuilder userInfoBuilder;

    public BladeSystemUserCompatController(BladeTokenUserStore tokenUserStore,
                                           SysPermissionService permissionService,
                                           BladeUserInfoBuilder userInfoBuilder) {
        this.tokenUserStore = tokenUserStore;
        this.permissionService = permissionService;
        this.userInfoBuilder = userInfoBuilder;
    }

    @GetMapping("/info")
    public BladeResult<Map<String, Object>> info(HttpServletRequest request) {
        String token = TokenHeaderResolver.resolve(request);
        if (token == null) {
            return BladeResult.fail("缺少访问令牌");
        }

        SysUser user = tokenUserStore.get(token);
        if (user == null) {
            return BladeResult.fail("未找到用户缓存信息");
        }

        Set<String> roles = permissionService.getRolePermission(user);
        Set<String> permissions = new HashSet<>(permissionService.getMenuPermission(user));
        if (user.isAdmin()) {
            // The generic administrator wildcard does not grant sharing powers in WVP.
            // Export only separately assigned exact menu permissions from the trusted store.
            Set<String> explicitPermissions = permissionService.getExplicitMenuPermission(user);
            for (String permission : WVP_SHARING_PERMISSIONS) {
                if (explicitPermissions.contains(permission)) permissions.add(permission);
            }
        }
        return BladeResult.success(userInfoBuilder.build(token, user, roles, permissions));
    }
}
