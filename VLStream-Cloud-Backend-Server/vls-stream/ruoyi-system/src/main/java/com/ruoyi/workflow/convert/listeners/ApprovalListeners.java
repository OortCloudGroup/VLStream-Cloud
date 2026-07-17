/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.workflow.convert.delegate.HttpTriggerDelegate;
import com.ruoyi.workflow.convert.node.HeaderOrParams;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workflow.service.LocationTaskWorkflowCallbackService;
import lombok.Data;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Data
@Component
public class ApprovalListeners implements TaskListener , ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(HttpTriggerDelegate.class);
    private FixedValue requestMethod;
    private FixedValue requestUrl;
    private FixedValue headers;  // 用于接收 headers 的 JSON 字符串
    private FixedValue params;   // 用于接收 params 的 JSON 字符串
    private FixedValue paramsType;
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApprovalListeners.applicationContext = applicationContext;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        // 检查必要的字段是否配置
        if (requestMethod == null || requestUrl == null) {
            log.debug("审批监听器：requestMethod 或 requestUrl 未配置，跳过执行");
            return;
        }

        HistoryService historyService = applicationContext.getBean(HistoryService.class);

        // 安全地获取流程变量，并进行空值检查
        String requestMethodValue = getStringValue(requestMethod, delegateTask, "requestMethod");
        String requestUrlValue = getStringValue(requestUrl, delegateTask, "requestUrl");
        String headersJson = getStringValue(headers, delegateTask, "headers");
        String paramsJson = getStringValue(params, delegateTask, "params");
        String paramsTypeValue = getStringValue(paramsType, delegateTask, "paramsType");

        // 验证必要参数
        if (requestMethodValue == null || requestUrlValue == null) {
            log.warn("审批监听器：requestMethod 或 requestUrl 值为空，跳过执行");
            return;
        }

        Gson gson = new Gson();
        List<HeaderOrParams> headerList = null;
        List<HeaderOrParams> paramsList = null;

        // 安全地解析 JSON
        try {
            if (headersJson != null && !headersJson.isEmpty()) {
                headerList = gson.fromJson(headersJson, new TypeToken<List<HeaderOrParams>>() {}.getType());
            }
            if (paramsJson != null && !paramsJson.isEmpty()) {
                paramsList = gson.fromJson(paramsJson, new TypeToken<List<HeaderOrParams>>() {}.getType());
            }
        } catch (Exception e) {
            log.error("审批监听器：解析 JSON 参数失败", e);
            return;
        }

        // 如果没有解析出数据，初始化为空列表
        if (headerList == null) {
            headerList = new java.util.ArrayList<>();
        }
        if (paramsList == null) {
            paramsList = new java.util.ArrayList<>();
        }

        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(delegateTask.getProcessInstanceId())
            .includeProcessVariables()
            .singleResult();
        // 根据请求类型执行不同的操作
        try {
            LocationTaskWorkflowCallbackService locationTaskCallback =
                applicationContext.getBean(LocationTaskWorkflowCallbackService.class);
            if (locationTaskCallback.supports(requestUrlValue)) {
                locationTaskCallback.handleApprovedTask(delegateTask, historicProcIns);
                return;
            }
            switch (requestMethodValue.toUpperCase()) {
                case "GET":
                case "DELETE":
                    executeGetRequest(requestMethodValue, requestUrlValue, headerList, paramsList, historicProcIns);
                    break;
                case "POST":
                case "PUT":
                    executePostRequest(requestMethodValue, requestUrlValue, headerList, paramsList, paramsTypeValue, historicProcIns);
                    break;
                default:
                    log.warn("审批监听器：不支持的请求方法: {}", requestMethodValue);
            }
        } catch (Exception e) {
            log.error("审批监听器：执行 HTTP 请求失败", e);
        }
    }

    /**
     * 安全地获取 FixedValue 的字符串值
     */
    private String getStringValue(FixedValue fixedValue, DelegateTask delegateTask, String paramName) {
        if (fixedValue == null) {
            return null;
        }
        try {
            Object value = fixedValue.getValue(delegateTask);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("审批监听器：获取参数 {} 失败", paramName, e);
            return null;
        }
    }

    /**
     * 执行 GET 请求
     */
    private void executeGetRequest(String requestMethodValue, String url, List<HeaderOrParams> headers, List<HeaderOrParams> params, HistoricProcessInstance historicProcIns) {
        // 拼接 GET 请求的 URL 和参数
        StringBuilder fullUrl = new StringBuilder(url);
        for (HeaderOrParams header : headers) {
            processBooleanParameter(header, historicProcIns);
        }
        if (params != null && !params.isEmpty()) {
            fullUrl.append("?");
            for (HeaderOrParams param : params) {
                processBooleanParameter(param, historicProcIns);
                fullUrl.append(param.getKey())
                    .append("=")
                    .append(param.getValue())
                    .append("&");
            }
            // 去除最后一个多余的 "&"
            fullUrl.deleteCharAt(fullUrl.length() - 1);
        }

        // 打印最终的 URL
        System.out.println("Executing GET request: " + fullUrl.toString());

        // 使用 HttpClient 发送请求示例
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpRequestBase http = requestMethodValue.equals("GET") ? new HttpGet(fullUrl.toString()) : new HttpDelete(fullUrl.toString());
            headers.forEach(header -> http.addHeader(new BasicHeader(header.getKey(), header.getValue())));

            headers.forEach(header -> System.out.println("header:" + header.getKey() + header.getValue()));

            try (CloseableHttpResponse response = httpClient.execute(http)) {
                System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
                // 处理响应内容...
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行 POST 请求
     */
    private void executePostRequest(String requestMethodValue, String url, List<HeaderOrParams> headers, List<HeaderOrParams> params, String paramsType, HistoricProcessInstance historicProcIns) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        for (HeaderOrParams header : headers) {
            processBooleanParameter(header, historicProcIns);
        }
        for (HeaderOrParams param : params) {
            processBooleanParameter(param, historicProcIns);
        }
        String body = getRequestBody(params, paramsType, historicProcIns);
        // 打印最终的请求 body

        System.out.println("Executing POST request: " + url);
        System.out.println("Request Body: " + body);
        // 使用 HttpClient 发送请求示例
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpEntityEnclosingRequestBase http = requestMethodValue.equals("POST") ? new HttpPost(url) : new HttpPut(url);
            ;
            // 直接在请求头中添加额外的参数
            http.addHeader("accesstoken", request.getHeader("accesstoken"));
            http.addHeader("tenantId", historicProcIns == null ? "" : historicProcIns.getTenantId());
            http.addHeader("appid", request.getHeader("appid"));
            http.addHeader("requestType", request.getHeader("requesttype"));
            http.addHeader("secretKey", request.getHeader("secretkey"));
            http.addHeader("Content-Type", "application/json");

            headers.forEach(header -> http.addHeader(new BasicHeader(header.getKey(), header.getValue())));
            http.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(http)) {
                System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
                // 处理响应内容...
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processBooleanParameter(HeaderOrParams param, HistoricProcessInstance historicProcIns) {
        if ("1".equals(param.getKeyType()) && historicProcIns != null) {
            Map<String, Object> processVariables = historicProcIns.getProcessVariables();
            Object o = processVariables.get(param.getValue());
            if (o != null) {
                String valueStr = o.toString();
                if ("1".equals(valueStr)) {
                    param.setValue("true");
                } else if ("0".equals(valueStr)) {
                    param.setValue("false");
                } else {
                    param.setValue(valueStr);
                }
            }
        }
    }

    private String getRequestBody(List<HeaderOrParams> params, String paramsType, HistoricProcessInstance historicProcIns) {
        if ("json".equalsIgnoreCase(paramsType)) {
            return convertParamsToJson(params, historicProcIns);
        } else if ("form".equalsIgnoreCase(paramsType)) {
            return convertParamsToFormData(params);
        } else {
            throw new IllegalArgumentException("Unsupported params type: " + paramsType);
        }
    }

    /**
     * 将参数转换为 JSON 格式
     */
    private String convertParamsToJson(List<HeaderOrParams> params, HistoricProcessInstance historicProcIns) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, Object> jsonMap = new HashMap<>();
        String accesstoken = request.getHeader("accesstoken");
        SysUser sysUser= RedisUtils.getCacheObject(accesstoken);

        IWfProcessService processService = applicationContext.getBean(IWfProcessService.class);
        jsonMap.put("processDetail", processService.queryProcessDetail(historicProcIns.getId(), null,sysUser,false));
        //SysUser sysUser = RedisUtils.getCacheObject(accesstoken);
        // 从请求头中获取参数

        jsonMap.put("procinsId", historicProcIns);

        for (HeaderOrParams param : params) {
            jsonMap.put(param.getKey(), param.getValue());
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            return mapper.writeValueAsString(jsonMap);  // 将参数列表转换为 JSON 字符串
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将参数转换为 x-www-form-urlencoded 格式
     */
    private String convertParamsToFormData(List<HeaderOrParams> params) {
        StringBuilder formData = new StringBuilder();
        for (HeaderOrParams param : params) {
            formData.append(param.getKey())
                .append("=")
                .append(param.getValue())
                .append("&");
        }
        // 去除最后一个多余的 "&"
        if (formData.length() > 0) {
            formData.deleteCharAt(formData.length() - 1);
        }
        return formData.toString();
    }
}
