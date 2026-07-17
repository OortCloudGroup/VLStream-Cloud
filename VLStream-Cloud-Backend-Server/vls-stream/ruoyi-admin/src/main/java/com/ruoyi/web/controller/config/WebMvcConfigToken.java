/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.config;


import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.framework.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * token 配置
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
public class WebMvcConfigToken implements WebMvcConfigurer {
    private final SecurityProperties securityProperties;
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    /**
     * Register one Sa-Token login check for every protected backend endpoint.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
            .addPathPatterns("/**")
            .excludePathPatterns(securityProperties.getExcludes());
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**");
    }
}
