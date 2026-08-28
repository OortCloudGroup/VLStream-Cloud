/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
    //
    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
        "AccessToken", "Content-Type", "Cookie", "appID",
        "requestType", "secretKey", "tenantid", "serverID"
    );

    /**
     * current info new Build
     * @param requestBuilder OkHttp Build
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
     * Get current HTTP object
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
     * object
     *
     * @param params parameter
     * @return object
     */
    public static Map<String, Object> createRequestBody(Map<String, Object> params) {
        // Map
        Map<String, Object> requestBody = new HashMap<>();
        // Set layer field
        requestBody.put("push_method", 2);
        requestBody.put("duct_code", params.getOrDefault("duct_code", ""));
        // only channel_code to null is empty ,
        Object channelCode = params.get("channel_code");
        if (channelCode != null && StringUtils.isNotBlank(channelCode.toString())) {
            requestBody.put("channel_code", channelCode);
        }
        // Build toarray
        List<Map<String, Object>> toList = new ArrayList<>();
        Map<String, Object> toItem = new HashMap<>();
        toItem.put("uid", params.getOrDefault("uid", ""));
        toItem.put("name", params.getOrDefault("name", ""));
        toList.add(toItem);
        requestBody.put("to", toList);

        // Build dataobject
        Map<String, Object> data = new HashMap<>();
        data.put("msg_title", params.getOrDefault("msg_title", ""));
        data.put("msg_content", params.getOrDefault("msg_content", ""));
        requestBody.put("data", data);

        // Build ex_dataobject
        Map<String, Object> exData = new HashMap<>();
        exData.put("instatmsg_types", "1");
        exData.put("procins_id", params.getOrDefault("procins_id", ""));
        exData.put("task_id", params.getOrDefault("task_id", ""));
        requestBody.put("ex_data", exData);

        return requestBody;
    }

    /**
     * parameterBuild
     *
     * @param ductCode
     * @param channelCode
     * @param uid user ID
     * @param name username
     * @param msgTitle
     * @param msgContent
     * @param instatmsgTypes
     * @param procinsId workflow instance ID
     * @param taskId taskID
     * @return Build parameter Map
     */
    public static Map<String, Object> buildParams(String ductCode, String channelCode, String uid, String name,
                                                  String msgTitle, String msgContent, String instatmsgTypes,
                                                  String procinsId, String taskId,String templateCode,String variableValue) {
        Map<String, Object> params = new HashMap<>();
        // layer parameter
        params.put("duct_code", ductCode);
        params.put("channel_code", channelCode);
        // toarrayparameter
        params.put("uid", uid);
        params.put("name", name);
        // dataobjectparameter
        params.put("msg_title", msgTitle);
        params.put("msg_content", msgContent);
        // ex_dataobjectparameter
        params.put("instatmsg_types", instatmsgTypes);
        params.put("procins_id", procinsId);
        params.put("task_id", taskId);
        params.put("template_code", templateCode);
        params.put("variable_value", variableValue);
        return params;
    }
}
