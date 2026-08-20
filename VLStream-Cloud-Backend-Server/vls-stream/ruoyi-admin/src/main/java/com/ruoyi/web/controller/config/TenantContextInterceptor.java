/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.config;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.common.enums.TenantType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 将 Sa-Token 会话里的租户放入当前请求线程，并在请求完成后强制清理。
 */
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private final TokenProperties tokenProperties;

    public TenantContextInterceptor(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContextHolder.clear();
        if (!TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType())) {
            TenantContextHolder.setTenantId(tokenProperties.getSingleTenantId());
            return true;
        }
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null && StringUtils.isNotBlank(loginUser.getTenantId())) {
                String requestTenantId = firstHeader(request, "tenantId", "tenantid", "Tenant-Id");
                if (StringUtils.isNotBlank(requestTenantId)
                    && !loginUser.getTenantId().equals(requestTenantId)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "tenantId 与当前会话租户不一致");
                    return false;
                }
                TenantContextHolder.setTenantId(loginUser.getTenantId());
            }
        } catch (Exception ignored) {
            // 公开接口没有本地会话，由具体业务在校验后显式建立租户上下文。
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        TenantContextHolder.clear();
    }

    private static String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
