/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.PlatformConstants;
import com.ruoyi.common.utils.OkHttpClientHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.WfForm;
import com.ruoyi.workflow.mapper.WfFormMapper;
import com.ruoyi.workflow.service.impl.WorkOrderAppServiceImpl;
import com.ruoyi.workflow.utils.UnifiedMessageUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.task.api.Task;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * 超时消息通知监听器
 * 处理审批节点超时后的消息推送
 */
@Slf4j
@Component
public class TimeoutNotificationListener implements JavaDelegate, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    // 通过 FieldExtension 注入的参数
    private FixedValue notificationUserId;
    private FixedValue priority;
    private FixedValue data;
    private FixedValue channelTypes;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        TimeoutNotificationListener.applicationContext = applicationContext;
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            log.info("超时消息通知监听器开始执行，流程实例ID: {}", execution.getProcessInstanceId());

            // 1. 获取当前活动的任务
            TaskService taskService = applicationContext.getBean(TaskService.class);
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(execution.getProcessInstanceId())
                    .list();

            if (tasks.isEmpty()) {
                log.info("超时消息通知监听器：任务已不存在（可能已被人工处理或其他超时处理器处理），跳过执行");
                return;
            }

            // 2. 获取参数值
            String userId = getParameterValue(notificationUserId, execution, "notificationUserId");
            Integer priorityValue = getIntParameterValue(priority, execution, "priority");
            String dataValue = getParameterValue(data, execution, "data");
            List<Integer> channelTypesValue = getListParameterValue(channelTypes, execution, "channelTypes");

            if (userId == null || userId.isEmpty()) {
                log.warn("超时消息通知监听器：notificationUserId 为空，跳过执行");
                return;
            }

            if (priorityValue == null || dataValue == null || dataValue.isEmpty()) {
                log.warn("超时消息通知监听器：priority 或 data 为空，跳过执行");
                return;
            }

            // 3. 构建并发送消息通知
            for (Task task : tasks) {
                sendTimeoutNotification(task, userId, priorityValue, dataValue, channelTypesValue);
            }

            log.info("超时消息通知监听器执行完成");

        } catch (Exception e) {
            log.error("超时消息通知监听器执行失败", e);
            // 不抛出异常，避免影响流程执行
        }
    }

    /**
     * 发送超时消息通知
     */
    private void sendTimeoutNotification(Task task, String userId, Integer priorityValue, String dataValue,
            List<Integer> channelTypesValue) {
        try {
            // 1. 获取请求头信息
            HttpServletRequest request = getCurrentRequest();
            String tenantId = null;
            if (request != null) {
                tenantId = request.getHeader("tenantid");
                if (tenantId == null || tenantId.isEmpty()) {
                    tenantId = request.getHeader(PlatformConstants.HEADER_TENANT_ID);
                }
            }

            // 如果无法从请求获取，尝试从任务获取
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = task.getTenantId();
            }

            // 2. 获取应用ID
            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();

            String applicationId = null;
            try {
                String category = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(task.getProcessDefinitionId())
                        .singleResult()
                        .getCategory();

                WorkOrderAppServiceImpl workOrderAppService = applicationContext.getBean(WorkOrderAppServiceImpl.class);
                applicationId = workOrderAppService.getById(category).getApplicationId();
            } catch (Exception e) {
                log.warn("获取应用ID失败: {}", e.getMessage());
            }

            // 调用 app/v1/relatesinfo 获取应用名称
            String appName = getAppName(applicationId);

            // 3. 处理表单数据
            FormDataResult formData = processFormData(task, dataValue);

            // 4. 构建推送请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("priority", priorityValue);
            requestBody.put("enable_instatmsg", true);
            if (channelTypesValue != null && !channelTypesValue.isEmpty()) {
                requestBody.put("channel_types", channelTypesValue);
            }
            requestBody.put("app_id", applicationId);
            requestBody.put("token", "37b74dcb91e74f51a9ecb35360a5cf19");
            requestBody.put("tenant_id", tenantId);

            // 构建 data 对象
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("msg_content", formData.getMsgContent());
            requestBody.put("data", dataMap);

            // 如果有附件URL，添加到请求体
            if (!formData.getAnnexUrls().isEmpty()) {
                requestBody.put("annex", formData.getAnnexUrls());
                log.debug("超时消息通知：添加附件URL，数量={}", formData.getAnnexUrls().size());
            }

            // 从流程变量获取 ex_data 参数
            Map<String, Object> variables = task.getProcessVariables();
            requestBody.put("msg_source", Integer.parseInt(String.valueOf(variables.getOrDefault("msg_source", 1))));
            Object appPackage = variables.get("app_package");
            Object jumpPath = variables.get("jump_path");
            Object jumpParams = variables.get("jump_params");

            // 解析应用包名：优先从请求头 appid/appID 查表获取，查不到则用流程变量兜底
            String resolvedAppPackage = null;
            try {
                HttpServletRequest req = getCurrentRequest();
                String appIdFromHeader = null;
                if (req != null) {
                    appIdFromHeader = req.getHeader("appid");
                    if (appIdFromHeader == null || appIdFromHeader.isEmpty()) {
                        appIdFromHeader = req.getHeader("appID");
                    }
                }
                if (appIdFromHeader != null) {
                    WorkOrderAppServiceImpl woService = applicationContext.getBean(WorkOrderAppServiceImpl.class);
                    resolvedAppPackage = woService.resolveAppPackageByApplicationId(appIdFromHeader);
                }
            } catch (Exception e) {
                log.warn("超时消息通知：解析 app_package 异常，将使用流程变量兜底", e);
            }

            // 构建 ex_data
            Map<String, Object> exDataMap = new HashMap<>();
            boolean hasExData = false;
            if (resolvedAppPackage != null) {
                exDataMap.put("app_package", resolvedAppPackage);
                hasExData = true;
            } else if (appPackage != null) {
                exDataMap.put("app_package", appPackage.toString());
                hasExData = true;
            }
            if (jumpPath != null) {
                exDataMap.put("jump_path", jumpPath.toString());
                hasExData = true;
            }
            if (jumpParams != null) {
                exDataMap.put("jump_params", jumpParams.toString());
                hasExData = true;
            }
            if (appName != null) {
                exDataMap.put("applabel", appName);
                hasExData = true;
            }
            if (hasExData) {
                requestBody.put("ex_data", exDataMap);
                log.debug("超时消息通知：添加ex_data参数，app_package={}, jump_path={}, jump_params={}, applabel={}",
                        appPackage, jumpPath, jumpParams, appName);
            }

            // 构建 to 数组（单个用户）
            List<Map<String, Object>> toList = new ArrayList<>();
            Map<String, Object> toItem = new HashMap<>();
            toItem.put("uid", userId);
            toList.add(toItem);
            requestBody.put("to", toList);

            // 5. 调用第三方接口推送消息
            sendNotification(requestBody);

        } catch (Exception e) {
            log.error("发送超时消息通知失败", e);
        }
    }

    /**
     * 处理表单数据
     */
    private FormDataResult processFormData(Task task, String originalData) {
        FormDataResult result = new FormDataResult(originalData);

        try {
            String formKey = getFormKeyFromTask(task);
            if (StringUtils.isBlank(formKey) || !formKey.startsWith("key_")) {
                return result;
            }

            String formId = formKey.substring(4);
            WfFormMapper wfFormMapper = applicationContext.getBean(WfFormMapper.class);
            WfForm wfForm = wfFormMapper.selectById(formId);

            if (wfForm == null || StringUtils.isBlank(wfForm.getContent())) {
                return result;
            }

            parseFormContentSafely(wfForm.getContent(), task.getProcessVariables(), result);

        } catch (Exception e) {
            log.error("处理表单数据时发生异常，将使用原始data值", e);
        }

        return result;
    }

    /**
     * 安全地解析表单内容
     */
    private void parseFormContentSafely(String content, Map<String, Object> variables, FormDataResult result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);
            JsonNode widgetList = root.path("widgetList");

            if (!widgetList.isArray()) {
                return;
            }

            StringBuilder inputContent = new StringBuilder();

            for (JsonNode widget : widgetList) {
                try {
                    String type = widget.path("type").asText();

                    if ("input".equals(type)) {
                        processInputWidget(widget, variables, inputContent);
                    } else if ("picture-upload".equals(type)) {
                        processPictureUploadWidget(widget, variables, result.getAnnexUrls());
                    }
                } catch (Exception e) {
                    log.warn("处理控件时发生异常，跳过该控件: {}", e.getMessage());
                }
            }

            if (inputContent.length() > 0) {
                result.setMsgContent(inputContent.toString() + "\n" + result.getMsgContent());
            }

        } catch (JsonProcessingException e) {
            log.error("解析表单JSON失败", e);
        }
    }

    private void processInputWidget(JsonNode widget, Map<String, Object> variables, StringBuilder inputContent) {
        String id = widget.path("id").asText();
        String label = widget.path("options").path("label").asText();

        Object value = variables.get(id);
        if (value != null) {
            if (inputContent.length() > 0) {
                inputContent.append("\n");
            }
            inputContent.append(label).append("：").append(value);
        }
    }

    private void processPictureUploadWidget(JsonNode widget, Map<String, Object> variables, List<String> annexUrls) {
        String id = widget.path("id").asText();
        Object value = variables.get(id);

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item instanceof Map) {
                    Object url = ((Map<?, ?>) item).get("url");
                    if (url != null) {
                        annexUrls.add(url.toString());
                    }
                }
            }
        }
    }

    /**
     * 获取当前HTTP请求对象
     */
    private HttpServletRequest getCurrentRequest() {
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

    /**
     * 调用第三方接口发送消息推送
     */
    private void sendNotification(Map<String, Object> requestBody) throws IOException {
        OkHttpClient client = OkHttpClientHolder.CLIENT;
        ObjectMapper mapper = new ObjectMapper();
        MediaType jsonType = MediaType.parse("application/json");
        org.springframework.core.env.Environment env = applicationContext
                .getBean(org.springframework.core.env.Environment.class);

        String pushUrl = env.getProperty("UnifiedMessagingSend.url") + "msg/v1/send/notice";
        String jsonBody = mapper.writeValueAsString(requestBody);
        log.info("超时消息通知推送请求：URL={}, Body={}", pushUrl, jsonBody);

        RequestBody body = RequestBody.create(jsonBody, jsonType);

        Request.Builder requestBuilder = new Request.Builder()
                .url(pushUrl)
                .post(body)
                .addHeader("Content-Type", "application/json");

        // 复制当前请求的请求头
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (headerName != null && (headerName.equalsIgnoreCase("appID") ||
                        headerName.equalsIgnoreCase("accesstoken") ||
                        headerName.equalsIgnoreCase("AccessToken") ||
                        headerName.equalsIgnoreCase("tenantid") ||
                        headerName.equalsIgnoreCase("tenantID") ||
                        headerName.equalsIgnoreCase("requestType") ||
                        headerName.equalsIgnoreCase("secretKey") ||
                        headerName.equalsIgnoreCase("serverID"))) {
                    requestBuilder.addHeader(headerName, request.getHeader(headerName));
                }
            }
        }

        Request httpRequest = requestBuilder.build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("超时消息通知推送成功：{}", responseBody);
            } else {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.warn("超时消息通知推送失败：statusCode={}, response={}", response.code(), responseBody);
            }
        }
    }

    private String getAppName(String applicationId) {
        return UnifiedMessageUtils.getAppName(applicationId, applicationContext);
    }

    /**
     * 获取参数值
     */
    private String getParameterValue(FixedValue fixedValue, DelegateExecution execution, String paramName) {
        if (fixedValue != null) {
            Object value = fixedValue.getValue(execution);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return null;
    }

    /**
     * 获取整数参数值
     */
    private Integer getIntParameterValue(FixedValue fixedValue, DelegateExecution execution, String paramName) {
        String value = getParameterValue(fixedValue, execution, paramName);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("参数 {} 格式错误：{}", paramName, value);
            }
        }
        return null;
    }

    /**
     * 获取List参数值
     */
    private List<Integer> getListParameterValue(FixedValue fixedValue, DelegateExecution execution, String paramName) {
        String value = getParameterValue(fixedValue, execution, paramName);
        if (value != null) {
            try {
                // 使用Gson反序列化JSON数组
                com.google.gson.Gson gson = new com.google.gson.Gson();
                return gson.fromJson(value,
                        new com.google.gson.reflect.TypeToken<List<Integer>>() {
                        }.getType());
            } catch (Exception e) {
                log.warn("参数 {} 解析错误：{}", paramName, value);
            }
        }
        return null;
    }

    /**
     * 从任务获取formKey
     * 优先从任务定义获取，如果为空则从流程定义中获取
     */
    private String getFormKeyFromTask(Task task) {
        // 1. 首先尝试从任务直接获取
        String formKey = task.getFormKey();
        if (StringUtils.isNotBlank(formKey)) {
            return formKey;
        }

        // 2. 如果任务formKey为空，从流程定义的StartEvent获取
        try {
            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();

            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            if (bpmnModel != null) {
                // 获取开始节点的formKey
                for (org.flowable.bpmn.model.Process process : bpmnModel.getProcesses()) {
                    for (org.flowable.bpmn.model.FlowElement flowElement : process.getFlowElements()) {
                        if (flowElement instanceof org.flowable.bpmn.model.StartEvent) {
                            String startFormKey = ((org.flowable.bpmn.model.StartEvent) flowElement).getFormKey();
                            if (StringUtils.isNotBlank(startFormKey)) {
                                log.debug("超时消息通知：从StartEvent获取formKey={}", startFormKey);
                                return startFormKey;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("超时消息通知：从流程定义获取formKey失败", e);
        }

        return null;
    }

    /**
     * 表单数据处理结果
     */
    @Data
    private static class FormDataResult {
        private String msgContent;
        private List<String> annexUrls = new ArrayList<>();

        public FormDataResult(String initialContent) {
            this.msgContent = initialContent;
        }
    }
}
