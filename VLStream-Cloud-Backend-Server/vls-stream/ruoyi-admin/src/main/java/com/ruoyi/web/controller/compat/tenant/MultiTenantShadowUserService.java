/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * 按 (tenant_id, platform_user_id) 创建并加载本地影子用户。
 */
@Service
public class MultiTenantShadowUserService {

    private final ISysUserService userService;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final TokenProperties tokenProperties;

    public MultiTenantShadowUserService(ISysUserService userService,
                                        SysRoleMapper roleMapper,
                                        SysRoleMenuMapper roleMenuMapper,
                                        TokenProperties tokenProperties) {
        this.userService = userService;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.tokenProperties = tokenProperties;
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized SysUser loadOrCreate(PlatformIdentity identity, String displayName) {
        String tenantId = require(identity.getTenantId(), "平台 tenantId 不能为空");
        String platformUserId = require(identity.getUserId(), "平台 userId 不能为空");
        return inTenant(tenantId, () -> {
            SysUser existing = findShadowUser(tenantId, platformUserId);
            if (existing != null) {
                return loadUser(existing.getUserId());
            }
            SysRole tenantRole = loadOrCreateTenantRole(tenantId);
            SysUser user = new SysUser();
            user.setUserId(String.valueOf(IdUtil.getSnowflakeNextId()));
            user.setTenantId(tenantId);
            user.setPlatformUserId(platformUserId);
            user.setUserName(platformAccount(platformUserId));
            user.setLoginId(platformUserId);
            user.setNickName(safeName(firstNonBlank(displayName, identity.getUserName()), user.getUserName()));
            user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString()));
            user.setUserType("sys_user");
            user.setStatus("0");
            user.setDelFlag("0");
            user.setSex("2");
            user.setEmail("");
            user.setPhonenumber("");
            user.setCreateBy("platform-sso");
            user.setRoleIds(new String[] {tenantRole.getRoleId()});
            try {
                userService.insertUser(user);
            } catch (DuplicateKeyException duplicateKeyException) {
                SysUser concurrentUser = findShadowUser(tenantId, platformUserId);
                if (concurrentUser == null) {
                    throw duplicateKeyException;
                }
                return loadUser(concurrentUser.getUserId());
            }
            return loadUser(user.getUserId());
        });
    }

    private SysUser findShadowUser(String tenantId, String platformUserId) {
        return userService.getOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getTenantId, tenantId)
            .eq(SysUser::getPlatformUserId, platformUserId), false);
    }

    private SysUser loadUser(String userId) {
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            throw new IllegalStateException("影子用户创建后无法加载");
        }
        return user;
    }

    private SysRole loadOrCreateTenantRole(String tenantId) {
        SysRole existing = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getRoleKey, tokenProperties.getMultiTenantRoleKey())
            .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }

        SysRole template = inTenant(tokenProperties.getSingleTenantId(), () -> roleMapper.selectOne(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tokenProperties.getSingleTenantId())
                .eq(SysRole::getRoleKey, tokenProperties.getMultiTenantRoleTemplateKey())
                .last("LIMIT 1")));
        if (template == null) {
            throw new IllegalStateException("未找到租户角色模板: " + tokenProperties.getMultiTenantRoleTemplateKey());
        }

        List<SysRoleMenu> templateMenus = inTenant(tokenProperties.getSingleTenantId(), () -> roleMenuMapper.selectList(
            new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, template.getRoleId())));

        SysRole role = new SysRole();
        role.setRoleId(String.valueOf(IdUtil.getSnowflakeNextId()));
        role.setTenantId(tenantId);
        role.setRoleName(tokenProperties.getMultiTenantRoleName());
        role.setRoleKey(tokenProperties.getMultiTenantRoleKey());
        role.setRoleSort(template.getRoleSort());
        role.setDataScope(template.getDataScope());
        role.setMenuCheckStrictly(template.getMenuCheckStrictly());
        role.setDeptCheckStrictly(template.getDeptCheckStrictly());
        role.setStatus("0");
        role.setDelFlag("0");
        role.setCreateBy("platform-sso");
        role.setRemark("统一平台多租户影子用户默认角色");
        roleMapper.insert(role);

        for (SysRoleMenu templateMenu : templateMenus) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(role.getRoleId());
            roleMenu.setTenantId(tenantId);
            roleMenu.setMenuId(templateMenu.getMenuId());
            roleMenuMapper.insert(roleMenu);
        }
        return role;
    }

    private static String platformAccount(String platformUserId) {
        return "p_" + sha256(platformUserId).substring(0, 28);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item & 0xff));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成平台用户账号", exception);
        }
    }

    private static String safeName(String value, String fallback) {
        String name = firstNonBlank(value, fallback);
        return name.length() > 30 ? name.substring(0, 30) : name;
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first.trim() : second;
    }

    private static String require(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static <T> T inTenant(String tenantId, TenantOperation<T> operation) {
        String previous = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            return operation.execute();
        } finally {
            TenantContextHolder.setTenantId(previous);
        }
    }

    private interface TenantOperation<T> {
        T execute();
    }
}
