/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.utils;

import com.ruoyi.common.utils.ApiHeaderUtil;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
public class ApiHeaderUtilTest {

    /**
     * 清理当前线程绑定的请求上下文，避免影响后续测试。
     */
    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 验证容器枚举小写请求头时，白名单请求头仍能被复制到 OkHttp 请求。
     */
    @Test
    public void transferHeadersShouldCopyAllowedHeadersWhenContainerLowercasesNames() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(
            "accesstoken", "content-type", "cookie", "appid",
            "requesttype", "secretkey", "tenantid", "serverid"
        )));
        Map<String, String> headers = new HashMap<>();
        headers.put("accesstoken", "token-value");
        headers.put("content-type", "application/json");
        headers.put("cookie", "SESSION=test");
        headers.put("appid", "app-id");
        headers.put("requesttype", "app");
        headers.put("secretkey", "secret-key");
        headers.put("tenantid", "tenant-id");
        headers.put("serverid", "server-id");
        when(httpRequest.getHeader(anyString())).thenAnswer(invocation ->
            headers.get(invocation.getArgument(0, String.class).toLowerCase(Locale.ROOT))
        );
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        Request.Builder requestBuilder = new Request.Builder().url("http://example.com").get();
        ApiHeaderUtil.transferHeaders(requestBuilder);
        Request request = requestBuilder.build();

        assertEquals("token-value", request.header("AccessToken"));
        assertEquals("application/json", request.header("Content-Type"));
        assertEquals("SESSION=test", request.header("Cookie"));
        assertEquals("app-id", request.header("appID"));
        assertEquals("app", request.header("requestType"));
        assertEquals("secret-key", request.header("secretKey"));
        assertEquals("tenant-id", request.header("tenantid"));
        assertEquals("server-id", request.header("serverID"));
    }
}
