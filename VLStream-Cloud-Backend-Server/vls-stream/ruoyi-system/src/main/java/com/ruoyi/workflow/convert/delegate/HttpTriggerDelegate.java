/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.workflow.convert.node.HeaderOrParams;
import com.ruoyi.workflow.service.IWfProcessService;
import lombok.Data;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.engine.HistoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.history.HistoricProcessInstance;
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

@Component
@Data
public class HttpTriggerDelegate implements JavaDelegate, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(HttpTriggerDelegate.class);
    private FixedValue requestMethod;
    private FixedValue requestUrl;
    private FixedValue headers; // headers JSON
    private FixedValue params; // params JSON
    private FixedValue paramsType;
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        HttpTriggerDelegate.applicationContext = applicationContext;
    }

    @Override
    public void execute(DelegateExecution execution) {
        HistoryService historyService = applicationContext.getBean(HistoryService.class);
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        // Get workflow variable
        String requestMethodValue = (String) requestMethod.getValue(execution);
        String requestUrlValue = (String) requestUrl.getValue(execution);
        String headersJson = (String) headers.getValue(execution);
        String paramsJson = (String) params.getValue(execution);
        String paramsTypeValue = (String) paramsType.getValue(execution);
        Gson gson = new Gson();
        List<HeaderOrParams> headerList = gson.fromJson(headersJson, new TypeToken<List<HeaderOrParams>>() {
        }.getType());
        List<HeaderOrParams> paramsList = gson.fromJson(paramsJson, new TypeToken<List<HeaderOrParams>>() {
        }.getType());
        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(execution.getProcessInstanceId())
                .includeProcessVariables()
                .singleResult();
        // Execute operation
        switch (requestMethodValue.toUpperCase()) {
            case "GET":
            case "DELETE":
                executeGetRequest(requestMethodValue, requestUrlValue, headerList, paramsList, historicProcIns);
                break;
            case "POST":
            case "PUT":
                executePostRequest(request, requestMethodValue, requestUrlValue, headerList, paramsList,
                        paramsTypeValue, historicProcIns);
                break;
            default:
                throw new IllegalArgumentException("Unsupported request method: " + requestMethodValue);
        }
    }

    /**
     * Execute GET
     */
    private void executeGetRequest(String requestMethodValue, String url, List<HeaderOrParams> headers,
            List<HeaderOrParams> params, HistoricProcessInstance historicProcIns) {
        // GET URL and parameter
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
            // after "&"
            fullUrl.deleteCharAt(fullUrl.length() - 1);
        }

        // URL
        System.out.println("Executing GET request: " + fullUrl.toString());

        // HttpClient
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpRequestBase http = requestMethodValue.equals("GET") ? new HttpGet(fullUrl.toString())
                    : new HttpDelete(fullUrl.toString());
            headers.forEach(header -> http.addHeader(new BasicHeader(header.getKey(), header.getValue())));

            headers.forEach(header -> System.out.println("header:" + header.getKey() + header.getValue()));

            try (CloseableHttpResponse response = httpClient.execute(http)) {
                System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
                // Process ...
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute POST
     */
    private void executePostRequest(HttpServletRequest request, String requestMethodValue, String url,
            List<HeaderOrParams> headers, List<HeaderOrParams> params, String paramsType,
            HistoricProcessInstance historicProcIns) {
        for (HeaderOrParams header : headers) {
            processBooleanParameter(header, historicProcIns);
        }
        for (HeaderOrParams param : params) {
            processBooleanParameter(param, historicProcIns);
        }
        String body = getRequestBody(params, paramsType, historicProcIns);
        // body

        System.out.println("Executing POST request: " + url);
        System.out.println("Request Body: " + body);
        // HttpClient
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpEntityEnclosingRequestBase http = requestMethodValue.equals("POST") ? new HttpPost(url)
                    : new HttpPut(url);
            // in in parameter
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
                // Process ...
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get formworkflow variable, if is boolean workflow variable value to 0,1 need to Convert
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

    private String getRequestBody(List<HeaderOrParams> params, String paramsType,
            HistoricProcessInstance historicProcIns) {
        if ("json".equalsIgnoreCase(paramsType)) {
            return convertParamsToJson(params, historicProcIns);
        } else if ("form".equalsIgnoreCase(paramsType)) {
            return convertParamsToFormData(params);
        } else {
            throw new IllegalArgumentException("Unsupported params type: " + paramsType);
        }
    }

    /**
     * parameterConvert to JSON
     */
    private String convertParamsToJson(List<HeaderOrParams> params, HistoricProcessInstance historicProcIns) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        Map<String, Object> jsonMap = new HashMap<>();
        String accesstoken = request.getHeader("accesstoken");
        SysUser sysUser = RedisUtils.getCacheObject(accesstoken);

        IWfProcessService processService = applicationContext.getBean(IWfProcessService.class);
        // workflow
        jsonMap.put("processDetail", processService.queryProcessDetail(historicProcIns.getId(), null, sysUser, false));
        // SysUser sysUser = RedisUtils.getCacheObject(accesstoken);
        // from in Get parameter

        jsonMap.put("procinsId", historicProcIns);

        for (HeaderOrParams param : params) {
            jsonMap.put(param.getKey(), param.getValue());
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            return mapper.writeValueAsString(jsonMap); // parameter Convert to JSON
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * parameterConvert to x-www-form-urlencoded
     */
    private String convertParamsToFormData(List<HeaderOrParams> params) {
        StringBuilder formData = new StringBuilder();
        for (HeaderOrParams param : params) {
            formData.append(param.getKey())
                    .append("=")
                    .append(param.getValue())
                    .append("&");
        }
        // after "&"
        if (formData.length() > 0) {
            formData.deleteCharAt(formData.length() - 1);
        }
        return formData.toString();
    }
}
