/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * sa-token configuration
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    private final SecurityProperties securityProperties;

    /**
     * sa-token
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TokenInterceptor keeps handling token compatibility; SaInterceptor enforces controller annotations.
        registry.addInterceptor(new SaInterceptor().isAnnotation(true))
            .addPathPatterns("/**")
            // need to
            .excludePathPatterns(securityProperties.getExcludes());
    }

    private boolean isValidAccessToken(String accessToken) {
        try {
            HttpRequest request = HttpRequest.post("http://192.168.60.75:32610/oort/oortcloud-cloud-classroom/user/v1/verifyToken")
                .header("Accept", "application/json")
                .header("AccessToken", accessToken);

            HttpResponse response = request.execute();
            String responseBody = response.body(); // Get

            // cn.hutool.json.JSONObjectParse
            JSONObject resultJson = JSONUtil.parseObj(responseBody);
            System.out.println("resultJson = " + resultJson.toString());
            int code = resultJson.getInt("code"); // JSONUtil getInt method
            if (code == 200) {
                return true; // AccessToken
            } else if (code == 4004) {
                log.warn("无效的AccessToken: {}", accessToken);
                return false; // AccessToken
            } else {
                log.error("未知错误，验证AccessToken时接收到异常响应: {}", resultJson);
                return false; // non- , to
            }
        } catch (Exception e) {
            log.error("验证AccessToken时发生异常", e);
            return false; // , to
        }
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        // Sa-Token integrate jwt ( )
        return new StpLogicJwtForSimple();
    }

    /**
     * interface ( bean userReplace )
     */
    @Bean
    public StpInterface stpInterface() {
        return new SaPermissionImpl();
    }

    /**
     * Customdao layer
     */
    @Bean
    public SaTokenDao saTokenDao() {
        return new PlusSaTokenDao();
    }
}
