/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.utils;

import okhttp3.Request;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ApiHeaderUtil {
    // 允许的请求头白名单
    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
        "AccessToken", "Content-Type", "Cookie", "appID",
        "requestType", "secretKey", "tenantid","serverID"
    );

    /**
     * 将当前请求的允许头信息复制到新请求构建器
     * @param requestBuilder OkHttp请求构建器
     */
    public static void transferHeaders(Request.Builder requestBuilder) {
        HttpServletRequest httpRequest = getCurrentRequest();
        if (httpRequest == null || requestBuilder == null) return;

        for (String headerName : ALLOWED_HEADERS) {
            String headerValue = httpRequest.getHeader(headerName);
            if (headerValue != null) {
                requestBuilder.addHeader(headerName, headerValue);
            }
        }
    }

    /**
     * 获取当前HTTP请求对象
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * 创建请求体对象
     *
     * @param params 请求体参数
     * @return 请求体对象
     */
    public static Map<String, Object> createRequestBody(Map<String, Object> params) {
        // 创建一个默认的 Map 来表示请求体结构
        Map<String, Object> requestBody = new HashMap<>();
        // 设置顶层字段
        requestBody.put("push_method", 2);
        requestBody.put("duct_code", params.getOrDefault("duct_code", ""));
        // 只有当 channel_code 不为 null 且不为空字符串时，才加入请求体
        Object channelCode = params.get("channel_code");
        if (channelCode != null && StringUtils.isNotBlank(channelCode.toString())) {
            requestBody.put("channel_code", channelCode);
        }
        // 构建to数组
        List<Map<String, Object>> toList = new ArrayList<>();
        Map<String, Object> toItem = new HashMap<>();
        toItem.put("uid", params.getOrDefault("uid", ""));
        toItem.put("name", params.getOrDefault("name", ""));
        toList.add(toItem);
        requestBody.put("to", toList);

        // 构建data对象
        Map<String, Object> data = new HashMap<>();
        data.put("msg_title", params.getOrDefault("msg_title", ""));
        data.put("msg_content", params.getOrDefault("msg_content", ""));
        requestBody.put("data", data);

        // 构建ex_data对象
        Map<String, Object> exData = new HashMap<>();
        exData.put("instatmsg_types", "1");
        exData.put("procins_id", params.getOrDefault("procins_id", ""));
        exData.put("task_id", params.getOrDefault("task_id", ""));
        requestBody.put("ex_data", exData);

        return requestBody;
    }

    /**
     * 封装参数构建逻辑
     *
     * @param ductCode       应用编码
     * @param channelCode    渠道编码
     * @param uid            接收用户ID
     * @param name           接收用户名称
     * @param msgTitle       消息标题
     * @param msgContent     消息内容
     * @param instatmsgTypes 消息类型
     * @param procinsId      流程实例ID
     * @param taskId         任务ID
     * @return 构建好的参数 Map
     */
    public static Map<String, Object> buildParams(String ductCode, String channelCode, String uid, String name,
                                                  String msgTitle, String msgContent, String instatmsgTypes,
                                                  String procinsId, String taskId,String templateCode,String variableValue) {
        Map<String, Object> params = new HashMap<>();
        // 顶层参数
        params.put("duct_code", ductCode);
        params.put("channel_code", channelCode);
        // to数组参数
        params.put("uid", uid);
        params.put("name", name);
        // data对象参数
        params.put("msg_title", msgTitle);
        params.put("msg_content", msgContent);
        // ex_data对象参数
        params.put("instatmsg_types", instatmsgTypes);
        params.put("procins_id", procinsId);
        params.put("task_id", taskId);
        params.put("template_code", templateCode);
        params.put("variable_value", variableValue);
        return params;
    }
}
