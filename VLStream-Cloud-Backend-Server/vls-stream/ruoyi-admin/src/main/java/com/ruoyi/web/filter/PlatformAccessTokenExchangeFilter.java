/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.filter;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.common.interceptor.TokenHeaderRequestWrapper;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.SecurityProperties;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import com.ruoyi.web.controller.compat.tenant.MultiTenantAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transparently adapts a platform AccessToken to the local Sa-Token session used by protected APIs.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PlatformAccessTokenExchangeFilter extends OncePerRequestFilter {

    private final TokenProperties tokenProperties;
    private final SecurityProperties securityProperties;
    private final BladeTokenUserStore tokenUserStore;
    private final MultiTenantAuthService multiTenantAuthService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PlatformAccessTokenExchangeFilter(TokenProperties tokenProperties,
                                             SecurityProperties securityProperties,
                                             BladeTokenUserStore tokenUserStore,
                                             MultiTenantAuthService multiTenantAuthService) {
        this.tokenProperties = tokenProperties;
        this.securityProperties = securityProperties;
        this.tokenUserStore = tokenUserStore;
        this.multiTenantAuthService = multiTenantAuthService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType())
            || isExcluded(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String incomingToken = TokenHeaderResolver.resolve(request);
        if (StringUtils.isBlank(incomingToken) || tokenUserStore.get(incomingToken) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String tenantId = firstHeader(request, "tenantId", "tenantid", "Tenant-Id");
            String localToken = multiTenantAuthService.resolveLocalToken(incomingToken, tenantId, request);
            filterChain.doFilter(new TokenHeaderRequestWrapper(request, localToken), response);
        } catch (Exception exception) {
            log.warn("平台 AccessToken 透明换票失败: path={}, reason={}", request.getRequestURI(), exception.getMessage());
            writeUnauthorized(response, exception.getMessage());
        }
    }

    private boolean isExcluded(HttpServletRequest request) {
        String path = request.getServletPath();
        if (StringUtils.isBlank(path)) {
            path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (StringUtils.isNotBlank(contextPath) && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
        }
        if (securityProperties.getExcludes() != null) {
            for (String pattern : securityProperties.getExcludes()) {
                if (pathMatcher.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void writeUnauthorized(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("success", false);
        body.put("msg", StringUtils.isBlank(reason) ? "平台访问令牌校验失败" : reason);
        response.getWriter().write(JSONUtil.toJsonStr(body));
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
