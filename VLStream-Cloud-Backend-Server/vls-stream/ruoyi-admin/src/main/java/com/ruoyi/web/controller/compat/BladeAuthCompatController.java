/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.vlstream.compat.BladeAuthInfo;
import com.ruoyi.vlstream.compat.BladePasswordDecoder;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.compat.SingleTenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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

    public BladeAuthCompatController(SysLoginService loginService,
                                     ISysUserService userService,
                                     BladePasswordDecoder passwordDecoder,
                                     BladeTokenUserStore tokenUserStore,
                                     BladeTokenSessionService tokenSessionService,
                                     @Value("${sa-token.timeout:86400}") long tokenTimeout) {
        this.loginService = loginService;
        this.userService = userService;
        this.passwordDecoder = passwordDecoder;
        this.tokenUserStore = tokenUserStore;
        this.tokenSessionService = tokenSessionService;
        this.tokenTimeout = tokenTimeout;
    }

    @PostMapping("/token")
    public BladeResult<BladeAuthInfo> token(@RequestParam Map<String, String> params) {
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
            TenantContextHolder.setTenantId(SingleTenant.DEFAULT_TENANT_ID);
            String password = passwordDecoder.decode(encryptedPassword);
            String token = loginService.login(account, password, params.get("code"), params.get("uuid"));
            SysUser user = userService.selectUserByUserName(account);
            if (user == null) {
                throw new IllegalStateException("本地用户不存在");
            }
            user.setTenantId(SingleTenant.DEFAULT_TENANT_ID);
            tokenUserStore.put(token, user, tokenTimeout);
            String userName = firstNonBlank(user.getUserName(), account);
            BladeAuthInfo authInfo = BladeAuthInfo.passwordToken(token, account, userName, SingleTenant.DEFAULT_TENANT_ID, tokenTimeout);
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
