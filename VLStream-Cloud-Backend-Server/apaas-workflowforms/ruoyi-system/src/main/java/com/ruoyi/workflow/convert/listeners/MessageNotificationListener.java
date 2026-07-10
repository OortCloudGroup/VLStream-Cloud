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
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
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
 * 消息通知节点  监听器
 * 处理 MessageNode 的消息推送和自动流转逻辑
 */
@Slf4j
@Component
public class MessageNotificationListener implements TaskListener, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    // 注入参数
    private FixedValue priority;
    private FixedValue data;
    private FixedValue verificationInterval;
    private FixedValue channelTypes;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        MessageNotificationListener.applicationContext = applicationContext;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            log.info("MessageNotificationListener 开始执行，taskId={}, taskName={}", delegateTask.getId(),
                    delegateTask.getName());

            // 【新增】检查是否需要重置验证次数（重复通知回退场景）
            RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
            if (delegateTask.getExecutionId() != null) {
                Boolean shouldReset = (Boolean) runtimeService.getVariable(delegateTask.getExecutionId(),
                        "messageNode_verifyCount_reset");
                if (Boolean.TRUE.equals(shouldReset)) {
                    log.info("检测到重复通知回退，重置验证次数");
                    runtimeService.removeVariable(delegateTask.getExecutionId(), "messageNode_verifyCount_reset");
                    // verifyCount 会在后续重新初始化为 0
                }
            }

            // 1. 获取参数
            Integer priorityValue = null;
            String dataValue = null;
            Integer intervalValue = 15;
            List<Integer> channelTypesValue = null;

            if (priority != null) {
                Object val = priority.getValue(delegateTask);
                if (val != null) {
                    try {
                        priorityValue = Integer.parseInt(val.toString().trim());
                    } catch (NumberFormatException e) {
                        log.warn("priority 格式错误: {}", val);
                    }
                }
            }

            if (data != null) {
                Object val = data.getValue(delegateTask);
                if (val != null) {
                    dataValue = val.toString().trim();
                }
            }

            if (verificationInterval != null) {
                Object val = verificationInterval.getValue(delegateTask);
                if (val != null) {
                    try {
                        intervalValue = Integer.parseInt(val.toString().trim());
                    } catch (NumberFormatException e) {
                        log.warn("verificationInterval 格式错误: {}", val);
                    }
                }
            }

            if (channelTypes != null) {
                Object val = channelTypes.getValue(delegateTask);
                if (val != null) {
                    try {
                        String channelTypesJson = val.toString().trim();
                        // 使用Gson反序列化JSON数组
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        channelTypesValue = gson.fromJson(channelTypesJson,
                                new com.google.gson.reflect.TypeToken<List<Integer>>() {
                                }.getType());
                    } catch (Exception e) {
                        log.warn("channelTypes 解析错误: {}", val);
                    }
                }
            }

            // 2. 获取通知对象 (Assignee)
            String assignee = delegateTask.getAssignee();
            // 去掉 MESSAGENODE_ 前缀获取真实 userId
            if (assignee != null && assignee.startsWith("MESSAGENODE_")) {
                assignee = assignee.substring("MESSAGENODE_".length());
            }
            if (StringUtils.isBlank(assignee)) {
                log.warn("MessageNotificationListener: Assignee 为空，无法发送通知");
                // 如果没有 assignee，可能无法继续，直接自动完成？
                autoCompleteTask(delegateTask);
                return;
            }

            // 3. 发送通知
            String msgNo = sendNotificationAndGetNo(delegateTask, assignee, priorityValue, dataValue,
                    channelTypesValue);

            // 4. 存储任务局部变量 (Local Variable)
            if (StringUtils.isNotBlank(msgNo)) {
                delegateTask.setVariableLocal("messageNode_msgNo", msgNo);
                delegateTask.setVariableLocal("messageNode_uid", assignee);
                delegateTask.setVariableLocal("messageNode_priority", priorityValue);
                delegateTask.setVariableLocal("messageNode_retryCount", 0);
                delegateTask.setVariableLocal("messageNode_verifyCount", 0); // 初始化验证次数
                // 标记该任务需要验证
                delegateTask.setVariableLocal("messageNode_needVerify", true);
            }

            // 5. 判断是否自动完成
            // 如果 priority 不是 0 或 1，或者发送失败没有 msgNo，直接自动完成
            if (priorityValue == null || (priorityValue != 0 && priorityValue != 1) || StringUtils.isBlank(msgNo)) {
                log.info("MessageNotificationListener: priority={}, msgNo={}, 标记为准备完成", priorityValue, msgNo);
                autoCompleteTask(delegateTask);
            } else {
                log.info("MessageNotificationListener: 等待验证，任务暂停。taskId={}", delegateTask.getId());
            }

        } catch (Exception e) {
            log.error("MessageNotificationListener 执行失败", e);
            // 异常情况下，为避免流程卡死，尝试自动完成？或者抛出异常让 Flowable 处理重试？
            // 这里选择记录日志，不阻断流程（Flowable 默认行为）
        }
    }

    private void autoCompleteTask(DelegateTask delegateTask) {
        // 1. 设置变量作为兜底，防止事务回调失败
        delegateTask.setVariableLocal("messageNode_readyToComplete", true);

        // final String taskId = delegateTask.getId();
        // try {
        // // 2. 注册事务同步回调，在事务提交后立即完成任务
        // org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
        // new org.springframework.transaction.support.TransactionSynchronization() {
        // @Override
        // public void afterCommit() {
        // try {
        // TaskService taskService = applicationContext.getBean(TaskService.class);
        // taskService.complete(taskId);
        // log.info("MessageNotificationListener: 任务 {} 已通过事务回调自动完成", taskId);
        // } catch (Exception e) {
        // log.error("MessageNotificationListener: 自动完成任务失败，将由 Scheduler 兜底。taskId={}",
        // taskId, e);
        // }
        // }
        // });
        // } catch (Exception e) {
        // log.warn("注册事务同步回调失败，将由 Scheduler 兜底。taskId={}", taskId, e);
        // }
    }

    private String sendNotificationAndGetNo(DelegateTask delegateTask, String assignee, Integer priorityValue,
            String dataValue, List<Integer> channelTypesValue) {
        try {
            // 构建请求体 (复用 ApprovalNotificationListener 逻辑)
            HttpServletRequest request = getCurrentRequest();
            String tenantId = null;
            if (request != null) {
                tenantId = request.getHeader("tenantid");
                if (tenantId == null || tenantId.isEmpty()) {
                    tenantId = request.getHeader(PlatformConstants.HEADER_TENANT_ID);
                }
            }
            if (tenantId == null)
                tenantId = delegateTask.getTenantId();

            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();
            String category = null;
            try {
                category = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(delegateTask.getProcessDefinitionId())
                        .singleResult()
                        .getCategory();
            } catch (Exception e) {
                log.warn("获取流程分类信息失败: {}", e.getMessage());
            }

            WorkOrderAppServiceImpl workOrderAppService = applicationContext.getBean(WorkOrderAppServiceImpl.class);
            String applicationId = workOrderAppService.getById(category).getApplicationId();
            // 调用 app/v1/relatesinfo 获取应用名称
            String appName = getAppName(applicationId);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("priority", priorityValue);
            requestBody.put("enable_instatmsg", true);
            if (channelTypesValue != null && !channelTypesValue.isEmpty()) {
                requestBody.put("channel_types", channelTypesValue);
            }
            requestBody.put("app_id", applicationId);
            requestBody.put("token", "37b74dcb91e74f51a9ecb35360a5cf19");
            requestBody.put("tenant_id", tenantId);

            Map<String, Object> dataMap = new HashMap<>();
            FormDataResult formData = processFormData(delegateTask, dataValue);
            dataMap.put("msg_content", formData.getMsgContent());
            requestBody.put("data", dataMap);

            if (!formData.getAnnexUrls().isEmpty()) {
                requestBody.put("annex", formData.getAnnexUrls());
                requestBody.put("file_source", 1);
            }

            // 从流程变量获取 ex_data 参数
            Map<String, Object> variables = delegateTask.getVariables();
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
                log.warn("消息通知：解析 app_package 异常，将使用流程变量兜底", e);
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
                log.debug("消息通知：添加ex_data参数，app_package={}, jump_path={}, jump_params={}, applabel={}",
                        appPackage, jumpPath, jumpParams, appName);
            }

            List<Map<String, Object>> toList = new ArrayList<>();
            Map<String, Object> toItem = new HashMap<>();
            toItem.put("uid", assignee);
            toList.add(toItem);
            requestBody.put("to", toList);

            // 发送请求
            String responseBody = sendRequest(requestBody);

            // 解析 msg_no
            if (StringUtils.isNotBlank(responseBody)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);
                if (root.path("code").asInt() == 200) {
                    JsonNode successList = root.path("data").path("success_list");
                    if (successList.isArray() && successList.size() > 0) {
                        return successList.get(0).path("no").asText();
                    }
                }
            }

        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
        return null;
    }

    // ... processFormData, getCurrentRequest, sendRequest 等辅助方法 (复用代码)
    // 为节省篇幅，这里需要完整实现这些方法

    private FormDataResult processFormData(DelegateTask delegateTask, String originalData) {
        FormDataResult result = new FormDataResult(originalData);
        try {
            String formKey = getFormKeyFromTask(delegateTask);
            if (StringUtils.isBlank(formKey) || !formKey.startsWith("key_")) {
                return result;
            }
            String formId = formKey.substring(4);
            WfFormMapper wfFormMapper = applicationContext.getBean(WfFormMapper.class);
            WfForm wfForm = wfFormMapper.selectById(formId);
            if (wfForm == null || StringUtils.isBlank(wfForm.getContent())) {
                return result;
            }
            parseFormContentSafely(wfForm.getContent(), delegateTask.getVariables(), result);
        } catch (Exception e) {
            log.error("处理表单数据异常", e);
        }
        return result;
    }

    private void parseFormContentSafely(String content, Map<String, Object> variables, FormDataResult result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);
            JsonNode widgetList = root.path("widgetList");
            if (!widgetList.isArray())
                return;

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
                    // ignore
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
            if (inputContent.length() > 0)
                inputContent.append("\n");
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
                    if (url != null)
                        annexUrls.add(url.toString());
                }
            }
        }
    }

    private String getFormKeyFromTask(DelegateTask delegateTask) {
        String formKey = delegateTask.getFormKey();
        if (StringUtils.isNotBlank(formKey))
            return formKey;
        try {
            ProcessEngineConfigurationImpl conf = Context.getProcessEngineConfiguration();
            BpmnModel bpmnModel = conf.getRepositoryService().getBpmnModel(delegateTask.getProcessDefinitionId());
            if (bpmnModel != null) {
                for (org.flowable.bpmn.model.Process process : bpmnModel.getProcesses()) {
                    for (org.flowable.bpmn.model.FlowElement flowElement : process.getFlowElements()) {
                        if (flowElement instanceof org.flowable.bpmn.model.StartEvent) {
                            String startFormKey = ((org.flowable.bpmn.model.StartEvent) flowElement).getFormKey();
                            if (StringUtils.isNotBlank(startFormKey))
                                return startFormKey;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取formKey失败", e);
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null)
                return attributes.getRequest();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String sendRequest(Map<String, Object> requestBody) throws IOException {
        OkHttpClient client = OkHttpClientHolder.CLIENT;
        ObjectMapper mapper = new ObjectMapper();
        MediaType jsonType = MediaType.parse("application/json");
        org.springframework.core.env.Environment env = applicationContext
                .getBean(org.springframework.core.env.Environment.class);
        String pushUrl = env.getProperty("UnifiedMessagingSend.url") + "msg/v1/send/notice";
        String jsonBody = mapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(jsonBody, jsonType);
        Request.Builder requestBuilder = new Request.Builder().url(pushUrl).post(body).addHeader("Content-Type",
                "application/json");

        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (headerName != null && (headerName.equalsIgnoreCase("appID")
                        || headerName.equalsIgnoreCase("accesstoken") || headerName.equalsIgnoreCase("tenantid"))) {
                    requestBuilder.addHeader(headerName, request.getHeader(headerName));
                }
            }
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        }
        return null;
    }

    private String getAppName(String applicationId) {
        return UnifiedMessageUtils.getAppName(applicationId, applicationContext);
    }

    @Data
    private static class FormDataResult {
        private String msgContent;
        private List<String> annexUrls = new ArrayList<>();

        public FormDataResult(String initialContent) {
            this.msgContent = initialContent;
        }
    }
}
