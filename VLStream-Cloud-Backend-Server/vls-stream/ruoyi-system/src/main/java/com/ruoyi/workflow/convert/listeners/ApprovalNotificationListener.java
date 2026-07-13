package com.ruoyi.workflow.convert.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.PlatformConstants;
import com.ruoyi.common.utils.OkHttpClientHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.WfForm;
import com.ruoyi.workflow.handler.MultiInstanceHandler;
import com.ruoyi.workflow.mapper.WfFormMapper;
import com.ruoyi.workflow.service.impl.WorkOrderAppServiceImpl;
import com.ruoyi.workflow.utils.UnifiedMessageUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
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
 * 审批节点消息推送  监听器
 * 当审批节点配置了 priority 和 data 参数时，自动推送消息给审批人
 */
@Slf4j
@Component
public class ApprovalNotificationListener implements TaskListener, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    // 通过 FieldExtension 注入的参数（字段名必须与 FieldExtension 的 fieldName 一致）
    // 使用 FixedValue 类型来接收 FieldExtension 的值
    private FixedValue priority;
    private FixedValue data;
    private FixedValue channelTypes;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        ApprovalNotificationListener.applicationContext = applicationContext;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            UserTask userTask = getUserTask(delegateTask);
            if (shouldSkipParallelMultiInstanceNotification(delegateTask, userTask)) {
                log.debug("审批节点消息推送：并行多实例非首个任务，跳过重复推送。taskId={}, loopCounter={}",
                    delegateTask.getId(), getLoopCounter(delegateTask));
                return;
            }
            // 1. 获取 priority 和 data 参数
            Integer priorityValue = null;
            String dataValue = null;
            List<Integer> channelTypesValue = null;

            if (priority != null) {
                Object priorityObj = priority.getValue(delegateTask);
                if (priorityObj != null) {
                    try {
                        priorityValue = Integer.parseInt(priorityObj.toString().trim());
                    } catch (NumberFormatException e) {
                        log.warn("审批节点消息推送：priority 格式错误，跳过推送。priority={}", priorityObj);
                        return;
                    }
                }
            }

            if (data != null) {
                Object dataObj = data.getValue(delegateTask);
                if (dataObj != null) {
                    dataValue = dataObj.toString().trim();
                }
            }

            if (channelTypes != null) {
                Object channelTypesObj = channelTypes.getValue(delegateTask);
                if (channelTypesObj != null) {
                    try {
                        String channelTypesJson = channelTypesObj.toString().trim();
                        // 使用Gson反序列化JSON数组
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        channelTypesValue = gson.fromJson(channelTypesJson,
                                new com.google.gson.reflect.TypeToken<List<Integer>>() {
                                }.getType());
                    } catch (Exception e) {
                        log.warn("审批节点消息推送：channelTypes 解析错误。channelTypes={}", channelTypesObj);
                    }
                }
            }

            // 2. 如果 priority 或 data 为空，则不执行推送
            if (priorityValue == null || dataValue == null || dataValue.isEmpty()) {
                log.debug("审批节点消息推送：priority 或 data 为空，跳过推送。priority={}, data={}", priority, data);
                return;
            }



            // 3. 获取当前审批人ID列表
            List<String> assigneeIds = resolveNotificationAssigneeIds(delegateTask, userTask);
            if (assigneeIds.isEmpty()) {
                log.warn("审批节点消息推送：未找到审批人，跳过推送");
                return;
            }

            // 4. 获取请求头信息
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                log.warn("审批节点消息推送：无法获取当前请求，跳过推送");
                return;
            }
            String tenantId = request.getHeader("tenantid");
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = request.getHeader(PlatformConstants.HEADER_TENANT_ID);
            }

            // 获取RepositoryService用于查询流程定义信息
            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();

            // 从流程定义中获取分类信息
            String category = null;
            try {
                // 获取流程定义信息
                category = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(delegateTask.getProcessDefinitionId())
                        .singleResult()
                        .getCategory();
            } catch (Exception e) {
                log.warn("获取流程分类信息失败: {}", e.getMessage());
            }
            String applicationId = null;
            // 如果仍然没有获取到分类信息，可以使用workOrderApp的applicationId作为备用
            WorkOrderAppServiceImpl workOrderAppService = applicationContext.getBean(WorkOrderAppServiceImpl.class);
            applicationId = workOrderAppService.getById(category).getApplicationId();
            // 调用 app/v1/relatesinfo 获取应用名称
            String appName = getAppName(applicationId);
            // 5. 构建推送请求体
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

            // 处理表单数据
            FormDataResult formData = processFormData(delegateTask, dataValue);
            dataMap.put("msg_content", formData.getMsgContent());
            requestBody.put("data", dataMap);

            // 如果有附件URL，添加到请求体
            if (!formData.getAnnexUrls().isEmpty()) {
                requestBody.put("annex", formData.getAnnexUrls());
                requestBody.put("file_source", 1);
                log.debug("审批节点消息推送：添加附件URL，数量={}", formData.getAnnexUrls().size());
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
                    resolvedAppPackage = workOrderAppService.resolveAppPackageByApplicationId(appIdFromHeader);
                }
            } catch (Exception e) {
                log.warn("审批节点消息推送：解析 app_package 异常，将使用流程变量兜底", e);
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
                log.debug("审批节点消息推送：添加ex_data参数，app_package={}, jump_path={}, jump_params={}, applabel={}",
                        appPackage, jumpPath, jumpParams, appName);
            }

            // 构建 to 数组
            List<Map<String, Object>> toList = new ArrayList<>();
            for (String assigneeId : assigneeIds) {
                Map<String, Object> toItem = new HashMap<>();
                toItem.put("uid", assigneeId);
                toList.add(toItem);
            }
            requestBody.put("to", toList);

            // 6. 调用第三方接口推送消息
            sendNotification(requestBody);

        } catch (Exception e) {
            log.error("审批节点消息推送失败", e);
            // 不抛出异常，避免影响流程执行
        }
    }

    /**
     * 获取当前审批人ID列表
     * 包括 assignee 和 candidates
     */
    private List<String> getAssigneeIds(DelegateTask delegateTask) {
        List<String> assigneeIds = new ArrayList<>();

        // 获取直接分配人
        String assignee = delegateTask.getAssignee();
        if (assignee != null && !assignee.trim().isEmpty()) {
            // 如果是多个用户，用逗号分隔
            String[] assignees = assignee.split(",");
            for (String assigneeId : assignees) {
                assigneeId = assigneeId.trim();
                if (!assigneeId.isEmpty() && !assigneeIds.contains(assigneeId)) {
                    assigneeIds.add(assigneeId);
                }
            }
        }

        // 获取候选人
        // Set<IdentityLink> candidates = delegateTask.getCandidates();
        // if (candidates != null) {
        // for (IdentityLink candidate : candidates) {
        // if (candidate.getUserId() != null && !candidate.getUserId().trim().isEmpty())
        // {
        // String userId = candidate.getUserId().trim();
        // if (!assigneeIds.contains(userId)) {
        // assigneeIds.add(userId);
        // }
        // }
        // }
        // }

        return assigneeIds;
    }

    /**
     * 获取本次消息推送接收人。
     */
    List<String> resolveNotificationAssigneeIds(DelegateTask delegateTask, UserTask userTask) {
        if (isParallelMultiInstance(userTask)) {
            Set<String> multiInstanceUserIds = getMultiInstanceHandler().getUserIds(userTask);
            if (!multiInstanceUserIds.isEmpty()) {
                return new ArrayList<>(multiInstanceUserIds);
            }
        }
        return getAssigneeIds(delegateTask);
    }

    /**
     * 并行多实例节点只允许首个任务发送聚合通知，避免同一批审批人重复调用推送接口。
     */
    boolean shouldSkipParallelMultiInstanceNotification(DelegateTask delegateTask, UserTask userTask) {
        if (!isParallelMultiInstance(userTask)) {
            return false;
        }

        Integer loopCounter = getLoopCounter(delegateTask);
        if (loopCounter != null) {
            return loopCounter > 0;
        }

        List<String> assigneeIds = resolveNotificationAssigneeIds(delegateTask, userTask);
        String assignee = delegateTask.getAssignee();
        return StringUtils.isNotBlank(assignee) && !assigneeIds.isEmpty() && !assignee.equals(assigneeIds.get(0));
    }

    private boolean isParallelMultiInstance(UserTask userTask) {
        return userTask != null && userTask.getLoopCharacteristics() != null
                && !userTask.getLoopCharacteristics().isSequential();
    }

    private Integer getLoopCounter(DelegateTask delegateTask) {
        Object loopCounter = delegateTask.getVariableLocal("loopCounter");
        if (loopCounter == null) {
            loopCounter = delegateTask.getVariable("loopCounter");
        }
        if (loopCounter instanceof Number) {
            return ((Number) loopCounter).intValue();
        }
        if (loopCounter != null) {
            try {
                return Integer.parseInt(loopCounter.toString());
            } catch (NumberFormatException e) {
                log.warn("审批节点消息推送：loopCounter 格式错误，按首个任务处理。loopCounter={}", loopCounter);
            }
        }
        return null;
    }

    private UserTask getUserTask(DelegateTask delegateTask) {
        try {
            String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
            if (StringUtils.isBlank(taskDefinitionKey)) {
                return null;
            }

            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            if (processEngineConfiguration == null) {
                return null;
            }
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(delegateTask.getProcessDefinitionId());
            if (bpmnModel == null) {
                return null;
            }

            FlowElement flowElement = bpmnModel.getFlowElement(taskDefinitionKey);
            if (flowElement instanceof UserTask) {
                return (UserTask) flowElement;
            }
        } catch (Exception e) {
            log.warn("审批节点消息推送：获取UserTask模型失败，使用当前任务审批人推送。taskId={}", delegateTask.getId(), e);
        }
        return null;
    }

    private MultiInstanceHandler getMultiInstanceHandler() {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(MultiInstanceHandler.class);
            } catch (Exception e) {
                log.debug("审批节点消息推送：未从Spring容器获取到MultiInstanceHandler，使用本地实例");
            }
        }
        return new MultiInstanceHandler();
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

        // 获取推送接口URL（从配置中获取，如果没有配置则使用默认值）
        String pushUrl = env.getProperty("UnifiedMessagingSend.url") + "msg/v1/send/notice";

        // 构建请求体
        String jsonBody = mapper.writeValueAsString(requestBody);
        log.info("审批节点消息推送请求：URL={}, Body={}", pushUrl, jsonBody);

        RequestBody body = RequestBody.create(jsonBody, jsonType);

        // 构建请求
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
                // 只复制必要的请求头
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

        // 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("审批节点消息推送成功：{}", responseBody);
            } else {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.warn("审批节点消息推送失败：statusCode={}, response={}", response.code(), responseBody);
            }
        }
    }

    private String getAppName(String applicationId) {
        return UnifiedMessageUtils.getAppName(applicationId, applicationContext);
    }

    // Getter 和 Setter 方法，用于 Flowable 注入 FieldExtension
    public FixedValue getPriority() {
        return priority;
    }

    public void setPriority(FixedValue priority) {
        this.priority = priority;
    }

    public FixedValue getData() {
        return data;
    }

    public void setData(FixedValue data) {
        this.data = data;
    }

    public FixedValue getChannelTypes() {
        return channelTypes;
    }

    public void setChannelTypes(FixedValue channelTypes) {
        this.channelTypes = channelTypes;
    }

    /**
     * 处理表单数据
     * 从表单配置中提取input和picture-upload控件的值
     *
     * @param delegateTask 任务委托对象
     * @param originalData 原始data值
     * @return 处理后的表单数据结果
     */
    private FormDataResult processFormData(DelegateTask delegateTask, String originalData) {
        FormDataResult result = new FormDataResult(originalData);

        try {
            // 1. 获取formKey并验证
            String formKey = getFormKeyFromTask(delegateTask);
            if (StringUtils.isBlank(formKey) || !formKey.startsWith("key_")) {
                log.debug("审批节点消息推送：FormKey为空或格式不正确，使用原始data值。formKey={}", formKey);
                return result;
            }

            // 2. 提取formId
            String formId = formKey.substring(4);
            log.debug("审批节点消息推送：提取formId={}", formId);

            // 3. 查询表单
            WfFormMapper wfFormMapper = applicationContext.getBean(WfFormMapper.class);
            WfForm wfForm = wfFormMapper.selectById(formId);

            if (wfForm == null || StringUtils.isBlank(wfForm.getContent())) {
                log.warn("审批节点消息推送：未找到表单或表单内容为空，formId={}，使用原始data值", formId);
                return result;
            }

            // 4. 解析表单内容
            log.debug("审批节点消息推送：开始解析表单内容");
            parseFormContentSafely(wfForm.getContent(), delegateTask.getVariables(), result);

        } catch (Exception e) {
            log.error("审批节点消息推送：处理表单数据时发生异常，将使用原始data值", e);
            // 异常时保持原始数据，不影响消息推送
        }

        return result;
    }

    /**
     * 安全地解析表单内容
     *
     * @param content   表单JSON内容
     * @param variables 流程变量
     * @param result    结果对象
     */
    private void parseFormContentSafely(String content, Map<String, Object> variables, FormDataResult result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);
            JsonNode widgetList = root.path("widgetList");

            if (!widgetList.isArray()) {
                log.warn("审批节点消息推送：widgetList不是数组格式");
                return;
            }

            StringBuilder inputContent = new StringBuilder();
            int inputCount = 0;
            int uploadCount = 0;

            // 遍历所有控件
            for (JsonNode widget : widgetList) {
                try {
                    String type = widget.path("type").asText();
                    if ("input".equals(type)) {
                        if (processInputWidget(widget, variables, inputContent)) {
                            inputCount++;
                        }
                    } else if ("picture-upload".equals(type)) {
                        if (processPictureUploadWidget(widget, variables, result.getAnnexUrls())) {
                            uploadCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("审批节点消息推送：处理单个控件时发生异常，跳过该控件。error={}", e.getMessage());
                    // 继续处理下一个控件
                }
            }

            // 组合最终的msg_content：所有input控件内容 + 原始dataValue
            if (inputContent.length() > 0) {
                // 如果有input内容，格式为：input内容\n原始值
                result.setMsgContent(inputContent.toString() + "\n" + result.getMsgContent());
            }
            // 如果没有input内容，保持原始值不变

            log.info("审批节点消息推送：表单解析完成，处理了{}个input控件，{}个picture-upload控件", inputCount, uploadCount);

        } catch (JsonProcessingException e) {
            log.error("审批节点消息推送：解析表单JSON失败", e);
        }
    }

    /**
     * 处理input类型控件
     *
     * @param widget       控件JSON节点
     * @param variables    流程变量
     * @param inputContent 用于收集input内容的StringBuilder
     * @return 是否成功处理
     */
    private boolean processInputWidget(JsonNode widget, Map<String, Object> variables, StringBuilder inputContent) {
        String id = widget.path("id").asText();
        String label = widget.path("options").path("label").asText();

        Object value = variables.get(id);
        if (value != null) {
            if (inputContent.length() > 0) {
                inputContent.append("\n");
            }
            inputContent.append(label).append("：").append(value);
            log.debug("审批节点消息推送：处理input控件 id={}, label={}, value={}", id, label, value);
            return true;
        }
        return false;
    }

    /**
     * 处理picture-upload类型控件
     *
     * @param widget    控件JSON节点
     * @param variables 流程变量
     * @param annexUrls 附件URL列表
     * @return 是否成功处理
     */
    private boolean processPictureUploadWidget(JsonNode widget, Map<String, Object> variables, List<String> annexUrls) {
        String id = widget.path("id").asText();
        Object value = variables.get(id);

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            int urlCount = 0;

            for (Object item : list) {
                if (item instanceof Map) {
                    Object url = ((Map<?, ?>) item).get("url");
                    if (url != null) {
                        annexUrls.add(url.toString());
                        urlCount++;
                    }
                }
            }

            if (urlCount > 0) {
                log.debug("审批节点消息推送：处理picture-upload控件 id={}, 提取了{}个URL", id, urlCount);
                return true;
            }
        }
        return false;
    }

    /**
     * 从任务获取formKey
     * 优先从任务定义获取，如果为空则从流程定义中获取
     */
    private String getFormKeyFromTask(DelegateTask delegateTask) {
        // 1. 首先尝试从任务直接获取
        String formKey = delegateTask.getFormKey();
        if (StringUtils.isNotBlank(formKey)) {
            return formKey;
        }

        // 2. 如果任务formKey为空，从流程定义的StartEvent获取
        try {
            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();

            BpmnModel bpmnModel = repositoryService.getBpmnModel(delegateTask.getProcessDefinitionId());
            if (bpmnModel != null) {
                // 获取开始节点的formKey
                for (org.flowable.bpmn.model.Process process : bpmnModel.getProcesses()) {
                    for (org.flowable.bpmn.model.FlowElement flowElement : process.getFlowElements()) {
                        if (flowElement instanceof org.flowable.bpmn.model.StartEvent) {
                            String startFormKey = ((org.flowable.bpmn.model.StartEvent) flowElement).getFormKey();
                            if (StringUtils.isNotBlank(startFormKey)) {
                                log.debug("审批节点消息推送：从StartEvent获取formKey={}", startFormKey);
                                return startFormKey;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("审批节点消息推送：从流程定义获取formKey失败", e);
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
