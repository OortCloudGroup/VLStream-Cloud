/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysLoginService;

import com.ruoyi.vlstream.test.compat.BladeAuthInfo;
import com.ruoyi.vlstream.test.compat.BladePasswordDecoder;
import com.ruoyi.vlstream.test.compat.BladeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.web.controller.compat.tenant.MultiTenantAuthService;

@RestController
@RequestMapping("/blade-auth")
public class BladeAuthCompatController {

    private static final String PASSWORD_GRANT_TYPE = "password";

    private final SysLoginService loginService;
    private final ISysUserService userService;
    private final BladePasswordDecoder passwordDecoder;
    private final BladeTokenUserStore tokenUserStore;
    private final BladeTokenSessionService tokenSessionService;
    private final long tokenTimeout;
    private final TokenProperties tokenProperties;
    private final MultiTenantAuthService multiTenantAuthService;

    public BladeAuthCompatController(SysLoginService loginService,
                                     ISysUserService userService,
                                     BladePasswordDecoder passwordDecoder,
                                     BladeTokenUserStore tokenUserStore,
                                     BladeTokenSessionService tokenSessionService,
                                     TokenProperties tokenProperties,
                                     MultiTenantAuthService multiTenantAuthService,
                                     @Value("${sa-token.timeout:86400}") long tokenTimeout) {
        this.loginService = loginService;
        this.userService = userService;
        this.passwordDecoder = passwordDecoder;
        this.tokenUserStore = tokenUserStore;
        this.tokenSessionService = tokenSessionService;
        this.tokenProperties = tokenProperties;
        this.multiTenantAuthService = multiTenantAuthService;
        this.tokenTimeout = tokenTimeout;
    }

    @PostMapping("/token")
    public BladeResult<BladeAuthInfo> token(@RequestParam Map<String, String> params) {
        if (TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType())) {
            return BladeResult.fail("多租户模式请使用统一平台登录");
        }
        String grantType = firstNonBlank(params.get("grantType"), PASSWORD_GRANT_TYPE);
        if (!PASSWORD_GRANT_TYPE.equalsIgnoreCase(grantType)) {
            return BladeResult.fail("不支持的授权类型");
        }

        String account = trimToNull(params.get("account"));
        String encryptedPassword = trimToNull(params.get("password"));
        if (account == null || encryptedPassword == null) {
            return BladeResult.fail("账号或密码不能为空");
        }

        try {
            TenantContextHolder.setTenantId(tokenProperties.getSingleTenantId());
            String password = passwordDecoder.decode(encryptedPassword);
            String token = loginService.login(account, password, params.get("code"), params.get("uuid"));
            SysUser user = userService.selectUserByUserName(account);
            if (user == null) {
                throw new IllegalStateException("本地用户不存在");
            }
            user.setTenantId(tokenProperties.getSingleTenantId());
            tokenUserStore.put(token, user, tokenTimeout);
            String userName = firstNonBlank(user.getUserName(), account);
            BladeAuthInfo authInfo = BladeAuthInfo.passwordToken(token, account, userName,
                tokenProperties.getSingleTenantId(), tokenTimeout);
            return BladeResult.success(authInfo);
        } catch (Exception ex) {
            return BladeResult.fail(firstNonBlank(ex.getMessage(), "登录失败"));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @PostMapping("/logout")
    public BladeResult<Void> logout(HttpServletRequest request) {
        String token = TokenHeaderResolver.resolve(request);
        if (token != null) {
            tokenSessionService.logoutByToken(token);
            tokenUserStore.remove(token);
            multiTenantAuthService.removeSession(token);
        }
        return BladeResult.success();
    }

    private static String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : trimToNull(second);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
