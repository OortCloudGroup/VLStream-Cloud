/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.config;


import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.framework.config.properties.SecurityProperties;
import com.ruoyi.web.controller.system.PublicFileController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * token configuration
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
public class WebMvcConfigToken implements WebMvcConfigurer {
    private static final Pattern PUBLIC_FILE_PATH = Pattern.compile(
        PublicFileController.PUBLIC_PATH + PublicFileController.FILE_ID_PATTERN);

    private final SecurityProperties securityProperties;
    private final TenantContextInterceptor tenantContextInterceptor;
    private final LocalSessionLifetimeInterceptor localSessionLifetimeInterceptor;
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    /**
     * Register one Sa-Token login check for every protected backend endpoint.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> requireLoginUnlessPublicRead()))
            .addPathPatterns("/**")
            .excludePathPatterns(securityProperties.getExcludes());
        registry.addInterceptor(localSessionLifetimeInterceptor).addPathPatterns("/**")
            .excludePathPatterns(securityProperties.getExcludes()).order(-10);
        registry.addInterceptor(tenantContextInterceptor).addPathPatterns("/**");
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**");
    }

    static boolean isPublicMarketRead(String method, String path) {
        return "GET".equals(method) && "/vlsCompute/public-market".equals(path);
    }

    static boolean isPublicRead(String method, String path) {
        return isPublicMarketRead(method, path) || isPublicFileRead(method, path);
    }

    public static boolean isPublicFileRead(String method, String path) {
        return ("GET".equals(method) || "HEAD".equals(method)) && PUBLIC_FILE_PATH.matcher(path).matches();
    }

    static String requestPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || path.isEmpty()
            ? request.getRequestURI().substring(request.getContextPath().length())
            : path;
    }

    static void requireLoginUnlessPublicRead() {
        if (!isPublicRead(cn.dev33.satoken.context.SaHolder.getRequest().getMethod(),
            cn.dev33.satoken.context.SaHolder.getRequest().getRequestPath())) StpUtil.checkLogin();
    }
}
