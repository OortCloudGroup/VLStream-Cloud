/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.framework.config.properties.SecurityProperties;
import com.ruoyi.framework.satoken.dao.PlusSaTokenDao;
import com.ruoyi.framework.satoken.service.SaPermissionImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * sa-token 配置
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    private final SecurityProperties securityProperties;

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TokenInterceptor keeps handling token compatibility; SaInterceptor enforces controller annotations.
        registry.addInterceptor(new SaInterceptor().isAnnotation(true))
            .addPathPatterns("/**")
            // 排除不需要拦截的路径
            .excludePathPatterns(securityProperties.getExcludes());
    }

    private boolean isValidAccessToken(String accessToken) {
        try {
            HttpRequest request = HttpRequest.post("http://192.168.60.75:32610/oort/oortcloud-cloud-classroom/user/v1/verifyToken")
                .header("Accept", "application/json")
                .header("AccessToken", accessToken);

            HttpResponse response = request.execute();
            String responseBody = response.body(); // 获取响应体字符串

            // 使用cn.hutool.json.JSONObject解析响应体
            JSONObject resultJson = JSONUtil.parseObj(responseBody);
            System.out.println("resultJson = " + resultJson.toString());
            int code = resultJson.getInt("code"); // 使用JSONUtil的getInt方法
            if (code == 200) {
                return true; // AccessToken有效
            } else if (code == 4004) {
                log.warn("无效的AccessToken: {}", accessToken);
                return false; // AccessToken无效
            } else {
                log.error("未知错误，验证AccessToken时接收到异常响应: {}", resultJson);
                return false; // 非预期响应，视为无效
            }
        } catch (Exception e) {
            log.error("验证AccessToken时发生异常", e);
            return false; // 发生网络或其他异常，视为无效
        }
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        // Sa-Token 整合 jwt (简单模式)
        return new StpLogicJwtForSimple();
    }

    /**
     * 权限接口实现(使用bean注入方便用户替换)
     */
    @Bean
    public StpInterface stpInterface() {
        return new SaPermissionImpl();
    }

    /**
     * 自定义dao层存储
     */
    @Bean
    public SaTokenDao saTokenDao() {
        return new PlusSaTokenDao();
    }
}
