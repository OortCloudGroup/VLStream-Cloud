/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.OkHttpClientHolder;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class UnifiedMessageUtils {

    private static final String[] ALLOWED_HEADERS = {
        "appID", "accesstoken", "AccessToken", "tenantid",
        "tenantID", "requestType", "secretKey", "serverID"
    };

    public static String getAppName(String applicationId, ApplicationContext applicationContext) {
        if (StringUtils.isBlank(applicationId)) return null;
        try {
            OkHttpClient client = OkHttpClientHolder.CLIENT;
            Environment env = applicationContext.getBean(Environment.class);
            String baseUrl = env.getProperty("UnifiedMessagingSend.url");
            if (StringUtils.isBlank(baseUrl)) {
                log.warn("UnifiedMessagingSend.url未配置");
                return null;
            }

            HttpUrl.Builder urlBuilder = HttpUrl.get(baseUrl + "app/v1/relatesinfo").newBuilder();
            urlBuilder.addQueryParameter("data_id", applicationId);

            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build()).get();

            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                for (String headerName : ALLOWED_HEADERS) {
                    String headerValue = request.getHeader(headerName);
                    if (headerValue != null) {
                        requestBuilder.addHeader(headerName, headerValue);
                    }
                }
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(jsonData);
                    JsonNode appInfo = root.path("data").path("app_info");
                    if (!appInfo.isMissingNode() && !appInfo.isNull()) {
                        return appInfo.path("app_name").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取应用名称失败", e);
        }
        return null;
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (IllegalStateException e) {
            log.debug("无法获取当前请求：{}", e.getMessage());
        }
        return null;
    }
}
