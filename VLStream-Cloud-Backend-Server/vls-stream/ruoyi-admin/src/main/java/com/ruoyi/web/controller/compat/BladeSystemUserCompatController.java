/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.system.service.SysPermissionService;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.compat.BladeUserInfoBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/blade-system/user")
public class BladeSystemUserCompatController {

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
        Set<String> permissions = permissionService.getMenuPermission(user);
        return BladeResult.success(userInfoBuilder.build(token, user, roles, permissions));
    }
}
