/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * notificationlistener
 * Process approvalnode after Push
 */
@Slf4j
@Component
public class TimeoutNotificationListener implements JavaDelegate, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    // FieldExtension parameter
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

            // 1. Get current task
            TaskService taskService = applicationContext.getBean(TaskService.class);
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(execution.getProcessInstanceId())
                    .list();

            if (tasks.isEmpty()) {
                log.info("超时消息通知监听器：任务已不存在（可能已被人工处理或其他超时处理器处理），跳过执行");
                return;
            }

            // 2. Get parameter value
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

            // 3. Build notification
            for (Task task : tasks) {
                sendTimeoutNotification(task, userId, priorityValue, dataValue, channelTypesValue);
            }

            log.info("超时消息通知监听器执行完成");

        } catch (Exception e) {
            log.error("超时消息通知监听器执行失败", e);
            // , workflowExecute
        }
    }

    /**
     * notification
     */
    private void sendTimeoutNotification(Task task, String userId, Integer priorityValue, String dataValue,
            List<Integer> channelTypesValue) {
        try {
            // 1. Get info
            String tenantId = task.getTenantId();

            // 2. Get ID
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

            // app/v1/relatesinfo Get
            String appName = getAppName(applicationId);

            // 3. Process formdata
            FormDataResult formData = processFormData(task, dataValue);

            // 4. Build Push
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("priority", priorityValue);
            requestBody.put("enable_instatmsg", true);
            if (channelTypesValue != null && !channelTypesValue.isEmpty()) {
                requestBody.put("channel_types", channelTypesValue);
            }
            requestBody.put("app_id", applicationId);
            requestBody.put("token", "37b74dcb91e74f51a9ecb35360a5cf19");
            requestBody.put("tenant_id", tenantId);

            // Build data object
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("msg_content", formData.getMsgContent());
            requestBody.put("data", dataMap);

            // if URL,
            if (!formData.getAnnexUrls().isEmpty()) {
                requestBody.put("annex", formData.getAnnexUrls());
                log.debug("超时消息通知：添加附件URL，数量={}", formData.getAnnexUrls().size());
            }

            // from workflow variableGet ex_data parameter
            Map<String, Object> variables = task.getProcessVariables();
            requestBody.put("msg_source", Integer.parseInt(String.valueOf(variables.getOrDefault("msg_source", 1))));
            Object appPackage = variables.get("app_package");
            Object jumpPath = variables.get("jump_path");
            Object jumpParams = variables.get("jump_params");

            // Parse : from appid/appID Get , workflow variable
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

            // Build ex_data
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

            // Build to array ( user)
            List<Map<String, Object>> toList = new ArrayList<>();
            Map<String, Object> toItem = new HashMap<>();
            toItem.put("uid", userId);
            toList.add(toItem);
            requestBody.put("to", toList);

            // 5. interfacePush
            sendNotification(requestBody);

        } catch (Exception e) {
            log.error("发送超时消息通知失败", e);
        }
    }

    /**
     * Process formdata
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
     * full Parse form
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
     * Get current HTTP object
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
     * interface Push
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

        // current
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
     * Get parameter value
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
     * Get parameter value
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
     * Get Listparameter value
     */
    private List<Integer> getListParameterValue(FixedValue fixedValue, DelegateExecution execution, String paramName) {
        String value = getParameterValue(fixedValue, execution, paramName);
        if (value != null) {
            try {
                // Gson JSONarray
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
     * from taskGet formKey
     * from task Get , if is empty from workflow definition in Get
     */
    private String getFormKeyFromTask(Task task) {
        // 1. from task Get
        String formKey = task.getFormKey();
        if (StringUtils.isNotBlank(formKey)) {
            return formKey;
        }

        // 2. if taskformKey is empty, from workflow definition StartEventGet
        try {
            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();

            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            if (bpmnModel != null) {
                // Get startnode formKey
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
     * formdataProcess
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
