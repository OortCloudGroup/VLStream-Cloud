package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.ApiHeaderUtil;
import com.ruoyi.common.utils.OkHttpClientHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.common.constant.TaskConstants;
import com.ruoyi.flowable.common.constant.UnifiedMessageUtil;
import com.ruoyi.flowable.common.enums.FlowComment;
import com.ruoyi.flowable.common.enums.ProcessStatus;
import com.ruoyi.flowable.common.enums.WorkOrderStatus;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.flowable.flow.CustomProcessDiagramGenerator;
import com.ruoyi.flowable.flow.FlowableUtils;
import com.ruoyi.flowable.utils.ModelUtils;
import com.ruoyi.flowable.utils.TaskUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleViewMapper;
import com.ruoyi.system.service.AjaxResult;
import com.ruoyi.workflow.domain.WfAttachment;
import com.ruoyi.workflow.domain.bo.WfSavePdfBo;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.mapper.WfAttachmentMapper;
import com.ruoyi.workflow.service.IWfCopyService;
import com.ruoyi.workflow.service.IWfInstanceService;
import com.ruoyi.workflow.service.IWfTaskService;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WfTaskServiceImpl extends FlowServiceFactory implements IWfTaskService {
    private final UserService sysUserService;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserRoleViewMapper sysUserRoleViewMapper;
    private final IWfCopyService copyService;
    private final WfAttachmentMapper wfAttachmentMapper;
    private final IWfInstanceService wfInstanceService;
    private final IWorkOrderService wfWorkOrderService;
    @Value("${dept.excludedUdid}")
    private String excludedUdid;
    @Value("${notification.slUrl2}")
    private String slUrl2;
    @Value("${notification.serviceID}")
    private String serviceID;
    @Value("${notification.secretKey}")
    private String secretKey;
    @Value("${notification.requestType}")
    private String requestType;

    @Value("${UnifiedMessagingSend.url}")
    private String msgUrl;

    /**
     * 推送消息
     *
     * @param recipients
     * @return
     */
    public AjaxResult sendNotification(List<String> recipients) {
        String Url = slUrl2 + "/api/v1/notify";
        // 组装要发送的数据
        JSONObject body = new JSONObject();
        // 推送事务的唯一 ID，调用方生成 UUID
        Random random = new Random();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String txId = "oort" + dateFormat.format(new Date()) + random.nextInt(10000);
        body.put("txId", txId);
        body.put("type", 1);

        JSONObject to = new JSONObject();
        to.put("zone", 2);
        body.put("to", to);

        String uid = "UID" + dateFormat.format(new Date()) + random.nextInt(10000);
        body.put("uid", uid);

        JSONObject notification = new JSONObject();
        notification.put("application", "oort流程中心");
        notification.put("title", "审核提醒");
        notification.put("content", "您有一条新任务需要处理");
        notification.put("recipients", recipients);
        body.put("notification", notification);

        String bodyStr = body.toString();
        System.out.println(bodyStr);

        // 使用 HttpClient 发送 POST 请求
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(Url);

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("requestType", requestType);
            httpPost.addHeader("serviceID", serviceID);
            httpPost.addHeader("secretKey", secretKey);
            httpPost.setEntity(new StringEntity(bodyStr, "UTF-8"));

            String response = "";
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                    System.out.println(response);
                } else {
                    System.err.println("请求出错：" + httpResponse.getStatusLine());
                }
            }

            // 模拟返回结果，根据实际需要调整
            if (!response.isEmpty()) {
                return AjaxResult.success(response);
            } else {
                return AjaxResult.error("请求失败");
            }

        } catch (IOException e) {
            e.printStackTrace();
            return AjaxResult.error("请求过程中发生异常");
        }
    }

    // 消息推送
    public void sendMessage(boolean isPushMessage, String userIds) {
        if (isPushMessage) {
            List<String> idCards = new ArrayList<>();
            String[] userIdArray = userIds.split(",");
            for (String id : userIdArray) {
                idCards.add(sysUserService.selectIdCardById(id));
            }
            if (!idCards.isEmpty()) {
                sendNotification(idCards);
            }
        }
    }

    /**
     * 对接统一消息推送
     *
     * @return
     */
    public void unifiedMessageSend(boolean isPushMessage, Map<String, Object> params) {
        if (isPushMessage) {
            OkHttpClient client = new OkHttpClient.Builder().build();

            // 使用 FastJSON 构建请求体
            String jsonBody = JSON.toJSONString(ApiHeaderUtil.createRequestBody(params));

            // 创建 MediaType 和 RequestBody
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, jsonBody);

            // 构建请求
            Request.Builder requestBuilder = new Request.Builder().url(msgUrl + "msg/v1/send").post(body);

            // 遍历当前请求的所有头信息，并添加到新的请求中
            ApiHeaderUtil.transferHeaders(requestBuilder);

            Request request = requestBuilder.build();

            // 执行请求并处理响应
            try (Response response = client.newCall(request).execute()) {
                System.out.println("Response Code: " + response.code());
                System.out.println("Response Body: " + response.body().string());
            } catch (Exception e) {
                throw new RuntimeException("Error executing HTTP request", e);
            }
        }
    }

    // /**
    // * 完成任务
    // *
    // * @param taskBo 请求实体参数
    // */
    // @Transactional(rollbackFor = Exception.class)
    // @Override
    // public void complete(WfTaskBo taskBo) {
    // Task task =
    // taskService.createTaskQuery().taskId(taskBo.getTaskId()).singleResult();
    // if (Objects.isNull(task)) {
    // throw new ServiceException("任务不存在");
    // }
    // // 获取 bpmn 模型
    // BpmnModel bpmnModel =
    // repositoryService.getBpmnModel(task.getProcessDefinitionId());
    // identityService.setAuthenticatedUserId(TaskUtils.getUserId());
    // if (DelegationState.PROCESSING.equals(task.getDelegationState())) {
    // taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(),
    // FlowComment.DELEGATE.getType(),
    // taskBo.getComment());
    // taskService.resolveTask(taskBo.getTaskId());
    // } else {
    // taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(),
    // FlowComment.NORMAL.getType(), taskBo
    // .getComment());
    // taskService.setAssignee(taskBo.getTaskId(), TaskUtils.getUserId());
    // if (ObjectUtil.isNotEmpty(taskBo.getVariables())) {
    // // 获取模型信息
    // String localScopeValue = ModelUtils.getUserTaskAttributeValue(bpmnModel,
    // task.getTaskDefinitionKey
    // (), ProcessConstants.PROCESS_FORM_LOCAL_SCOPE);
    // boolean localScope = Convert.toBool(localScopeValue, false);
    // taskService.complete(taskBo.getTaskId(), taskBo.getVariables(), localScope);
    // } else {
    // taskService.complete(taskBo.getTaskId());
    // }
    // }
    // // 设置任务节点名称
    // taskBo.setTaskName(task.getName());
    // // 处理下一级审批人
    // if (StringUtils.isNotBlank(taskBo.getNextUserIds())) {
    // this.assignORCandidateUserNextUsers(bpmnModel, taskBo.getProcInsId(),
    // taskBo.getNextUserIds(), taskBo
    // .isPushMessage());
    // }
    // // 处理抄送用户
    // if (!copyService.makeCopy(taskBo)) {
    // throw new RuntimeException("抄送任务失败");
    // }
    // }

    /**
     * 完成任务
     *
     * @param taskBo 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(WfTaskBo taskBo) {
        String token = AuthorizationInterceptor.getToken();
        SysUser user = RedisUtils.getCacheObject(token);
        Task task = taskService.createTaskQuery().taskTenantId(user.getTenantId()).taskId(taskBo.getTaskId())
                .singleResult();
        if (Objects.isNull(task)) {
            throw new ServiceException("任务不存在");
        }
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(user.getTenantId()).processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (processInstance.isSuspended()) {
            // 流程已挂起，禁止操作
            throw new RuntimeException("流程实例已被挂起，无法继续操作");
        }
        // 获取 bpmn 模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        if (user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrder::getProcInsId, taskBo.getProcInsId());
        WorkOrder one = wfWorkOrderService.getOne(queryWrapper);
        runtimeService.setVariable(taskBo.getProcInsId(), "currentAssignee", user.getUserId());
        // 获取是否需要消息推送
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
        if (ObjectUtil.isNotNull(runtimeService.getVariable(taskBo.getProcInsId(), "flowDirection"))) {
            if (StringUtils.isNotBlank(taskBo.getNextUserIds()) || ObjectUtil.isNotEmpty(taskBo.getCandidateUsers())
                    || ObjectUtil.isNotEmpty(taskBo.getCandidateGroups())) {
                runtimeService.setVariable(taskBo.getProcInsId(), "nextUserIds", taskBo.getNextUserIds());
            } else {
                // 判断是不是局领导提交
                SysUser sysUser = sysUserMapper.selectUserById(user.getUserId());
                SysDeptView sysDeptView = sysDeptMapper.selectVoById(sysUser.getDeptId());
                if (sysDeptView.getDeptId().equals(excludedUdid)) { // 属于局领导，直接结束流程
                    runtimeService.setVariable(taskBo.getProcInsId(), "flowDirection", "end");
                } else { // 不属于局领导，获取所有领导并写入
                    List<SysUser> leaders = sysUserService.getLeaders(user.getUserId());
                    String join = leaders.stream().map(SysUser::getUserId).collect(Collectors.joining(","));
                    this.assignORCandidateUserNextUsers(bpmnModel, taskBo.getProcInsId(), join,
                            taskBo.getCandidateUsers(), taskBo.getCandidateGroups(), notifyAllSteps, user);
                    runtimeService.setVariable(taskBo.getProcInsId(), "nextUserIds", join);
                }
            }
        }

        identityService.setAuthenticatedUserId(user.getUserId());
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(), FlowComment.DELEGATE.getType(),
                    taskBo.getComment());
            taskService.resolveTask(taskBo.getTaskId());
        } else {
            if (StringUtils.isNotBlank(taskBo.getCallbackType())) {
                taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(), taskBo.getCallbackType(),
                        taskBo.getComment());
            } else {
                taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(), FlowComment.NORMAL.getType(),
                        taskBo.getComment());
            }

            taskService.setAssignee(taskBo.getTaskId(), user.getUserId());
            // taskService.setAssignee(taskBo.getTaskId(), TaskUtils.getUserId());
            if (ObjectUtil.isNotEmpty(taskBo.getVariables())) {
                // 获取模型信息
                String localScopeValue = ModelUtils.getUserTaskAttributeValue(bpmnModel, task.getTaskDefinitionKey(),
                        ProcessConstants.PROCESS_FORM_LOCAL_SCOPE);
                boolean localScope = Convert.toBool(localScopeValue, false);
                taskService.complete(taskBo.getTaskId(), taskBo.getVariables(), localScope);
            } else {
                taskService.complete(taskBo.getTaskId());
            }
        }

        taskBo.setPushMessage(notifyAllSteps);
        List<Task> newTaskList = taskService.createTaskQuery().taskTenantId(user.getTenantId())
                .processInstanceId(taskBo.getProcInsId()).orderByTaskCreateTime().desc().list();
        Task newTask = newTaskList.stream().filter(tasks -> tasks.getAssignee().equals(user.getUserId())).findFirst()
                .orElse(null);
        if (ObjectUtil.isNotNull(newTask) && taskBo.isAcceptance()) {
            wfWorkOrderService.updateWorkOrderToPending(newTask, WorkOrderStatus.RETURN.getStatus(), null);
        }
        if (ObjectUtil.isNotNull(newTask) && StringUtils.isNotBlank(taskBo.getCallbackType())) {
            wfWorkOrderService.updateWorkOrderToPending(newTask, WorkOrderStatus.TO_BE_EVALUATED.getStatus(), null);
        }
//        if (ObjectUtil.isNotNull(newTask) && taskBo.isPushMessage()) {
//            buildAndSendUnifiedMessage(newTask, newTask.getAssignee(), notifyAllSteps, user);
//        }
        if (ObjectUtil.isNotNull(newTask) && !taskBo.isAcceptance() && StringUtils.isBlank(taskBo.getCallbackType())
                && ObjectUtil.isNotNull(one)
                ) {  //&& one.getWorkorderStatus().equals(WorkOrderStatus.PROCESSING.getStatus())
            wfWorkOrderService.updateWorkOrderToPending(newTask, WorkOrderStatus.PROCESSING.getStatus(), null);
        }

        // 设置任务节点名称
        taskBo.setTaskName(task.getName());
        // 处理下一级审批人
        if (StringUtils.isNotBlank(taskBo.getNextUserIds()) || ObjectUtil.isNotEmpty(taskBo.getCandidateUsers())
                || ObjectUtil.isNotEmpty(taskBo.getCandidateGroups())) { // 有指定审批人
            // 防止用户在最后一个节点还设置下一个节点的审批人
            ProcessInstance processInstance2 = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(taskBo.getProcInsId()).singleResult();
            if (processInstance2 == null) {
                throw new ServiceException("当前审批人为最后一个节点，无下一个节点");
            }
            if (ObjectUtil.isNull(runtimeService.getVariable(taskBo.getProcInsId(), "flowDirection"))) {
                this.assignORCandidateUserNextUsers(bpmnModel, taskBo.getProcInsId(), taskBo.getNextUserIds(),
                        taskBo.getCandidateUsers(), taskBo.getCandidateGroups(), notifyAllSteps, user);
            }
        }
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(user.getTenantId()).processInstanceId(taskBo.getProcInsId()).singleResult();
        if (historicProcessInstance.getEndTime() != null) {
            wfWorkOrderService.updateWorkOrderToPending(
                historicProcessInstance.getId(),
                    WorkOrderStatus.COMPLETED.getStatus(), null);
        }
        if (!copyService.makeCopy(taskBo, user)) {
            throw new RuntimeException("抄送任务失败");
        }
    }

    /**
     * 拒绝任务
     *
     * @param taskBo
     * @param user
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void taskReject(WfTaskBo taskBo, SysUser user) {
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskTenantId(user.getTenantId()).taskId(taskBo.getTaskId())
                .singleResult();
        if (ObjectUtil.isNull(task)) {
            throw new RuntimeException("获取任务信息异常！");
        }
        if (task.isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // 获取流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(user.getTenantId()).processInstanceId(taskBo.getProcInsId()).singleResult();
        if (processInstance == null) {
            throw new RuntimeException("流程实例不存在，请确认！");
        }
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(user.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                .singleResult();

        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // 添加审批意见
        taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(), FlowComment.REJECT.getType(),
                taskBo.getComment());
        // 设置流程状态为已终结
        runtimeService.setVariable(processInstance.getId(), ProcessConstants.PROCESS_STATUS_KEY,
                ProcessStatus.TERMINATED.getStatus());
        // 将拒绝节点的处理人信息存储到流程变量中
        runtimeService.setVariable(taskBo.getProcInsId(), "rejectAssignee", user.getUserId());
        runtimeService.setVariable(taskBo.getProcInsId(), "rejectTaskId", task.getId());
        runtimeService.setVariable(taskBo.getProcInsId(), "rejectTaskName", task.getName());
        // 获取所有节点信息
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        EndEvent endEvent = ModelUtils.getEndEvent(bpmnModel);
        // 获取发起人的用户ID
        String initiatorId = (String) runtimeService.getVariable(processInstance.getId(), "initiator");
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getProcessInstanceId(), "notifyAllSteps");
        // sendMessage(notifyAllSteps, initiatorId);

        // 调用统一消息推送方法
//        buildAndSendUnifiedMessage(task, initiatorId, notifyAllSteps, user);

        // 终止流程
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(task.getProcessInstanceId()).list();
        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());
        runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                .moveExecutionsToSingleActivityId(executionIds, endEvent.getId()).changeState();
        // 处理抄送用户
        taskBo.setPushMessage(notifyAllSteps);
        if (!copyService.makeCopy(taskBo, user)) {
            throw new RuntimeException("抄送任务失败");
        }
    }

    /**
     * 构建 content 值
     *
     * @param processName      流程名称
     * @param processVariables 流程变量
     * @return 动态生成的 content 值
     */
    private String buildContent(String processName, Map<String, Object> processVariables, String receiveUserName) {
        // 获取特殊流程的表单项 ID 映射
        Map<String, String[]> processVariableIds = UnifiedMessageUtil.getProcessVariableIds();

        // 判断是否是特殊流程
        if (processVariableIds.containsKey(processName)) {
            // 获取当前流程的表单项 ID
            String[] variableIds = processVariableIds.get(processName);
            String typeVariableId = variableIds[0];
            String nameVariableId = variableIds[1];

            // 获取类型和名称
            String type = (String) processVariables.getOrDefault(typeVariableId, "默认类型");
            String name = (String) processVariables.getOrDefault(nameVariableId, "默认名称");

            // 拼接 content 值
            return type + name + processName;
        } else {
            // 非特殊流程，content 值为流程名称
            return receiveUserName + processName;
        }
    }

    /**
     * 退回任务
     *
     * @param bo 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void taskReturn(WfTaskBo bo, SysUser sysUser) {
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        if (ObjectUtil.isNull(task)) {
            throw new RuntimeException("获取任务信息异常！");
        }
        if (task.isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        // 获取流程模型信息
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // 获取当前任务节点元素
        FlowElement source = ModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        // 获取跳转的节点元素
        FlowElement target = ModelUtils.getFlowElementById(bpmnModel, bo.getTargetKey());
        // 从当前节点向前扫描，判断当前节点与目标节点是否属于串行，若目标节点是在并行网关上或非同一路线上，不可跳转
        boolean isSequential = ModelUtils.isSequentialReachable(source, target, new HashSet<>());
        if (!isSequential) {
            throw new RuntimeException("当前节点相对于目标节点，不属于串行关系，无法回退");
        }

        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需退回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null,
                null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // 设置回退意见
        for (String currentTaskId : currentTaskIds) {
            taskService.addComment(currentTaskId, task.getProcessInstanceId(), FlowComment.REBACK.getType(),
                    bo.getComment());
        }

        try {
            // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetKey 跳转到的节点(1)
            runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentIds, bo.getTargetKey()).changeState();
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new RuntimeException("无法取消或开始活动");
        }
        // 设置任务节点名称
        bo.setTaskName(task.getName());
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getProcessInstanceId(), "notifyAllSteps");
        bo.setPushMessage(notifyAllSteps);
        // 处理抄送用户
        if (!copyService.makeCopy(bo, sysUser)) {
            throw new RuntimeException("抄送任务失败");
        }
        // 获取目标节点的任务办理人ID
        Task targetTask = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(task.getProcessInstanceId()).taskDefinitionKey(bo.getTargetKey()).singleResult();
        // 1. 更新工单状态
        wfWorkOrderService.updateWorkOrderToPending(targetTask, WorkOrderStatus.RETURNED.getStatus(), null);
        if (targetTask != null) {
            String targetAssignee = targetTask.getAssignee();
            // sendMessage(notifyAllSteps, targetAssignee);

            // 调用统一消息推送方法
//            buildAndSendUnifiedMessage(task, targetAssignee, notifyAllSteps, sysUser);
        } else {
            throw new RuntimeException("未找到目标节点的任务");
        }
    }

    /**
     * 获取所有可回退的节点
     *
     * @param bo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<FlowElement> findReturnTaskList(WfTaskBo bo) {
        SysUser user = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskTenantId(user.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(user.getTenantId())
                .processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取流程模型信息
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // 查询历史节点实例
        List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType(BpmnXMLConstants.ELEMENT_TASK_USER)
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .list();
        List<String> activityIdList = activityInstanceList.stream()
                .map(HistoricActivityInstance::getActivityId)
                .filter(activityId -> !StringUtils.equals(activityId,
                        task.getTaskDefinitionKey()))
                .distinct()
                .collect(Collectors.toList());
        // 获取当前任务节点元素
        FlowElement source = ModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        List<FlowElement> elementList = new ArrayList<>();
        for (String activityId : activityIdList) {
            FlowElement target = ModelUtils.getFlowElementById(bpmnModel, activityId);
            boolean isSequential = ModelUtils.isSequentialReachable(source, target, new HashSet<>());
            if (isSequential) {
                elementList.add(target);
            }
        }
        return elementList;
    }

    /**
     * 删除任务
     *
     * @param bo 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteTask(WfTaskBo bo) {
        // todo 待确认删除任务是物理删除任务 还是逻辑删除，让这个任务直接通过？
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        taskService.deleteTask(bo.getTaskId(), bo.getComment());
    }

    /**
     * 认领/签收任务
     *
     * @param taskBo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(WfTaskBo taskBo) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(taskBo.getTaskId())
                .singleResult();
        wfWorkOrderService.updateWorkOrderToPending(task, WorkOrderStatus.PROCESSING.getStatus(), sysUser.getUserId());
        if (Objects.isNull(task)) {
            throw new ServiceException("任务不存在");
        }
        taskService.claim(taskBo.getTaskId(), sysUser.getUserId());
    }

    /**
     * 取消认领/签收任务
     *
     * @param bo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unClaim(WfTaskBo bo) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        wfWorkOrderService.updateWorkOrderToPending(task, WorkOrderStatus.PENDING_ORDERS.getStatus(), "no");
        taskService.unclaim(bo.getTaskId());
    }

    // /**
    // * 委派任务
    // *
    // * @param bo 请求实体参数
    // */
    // @Override
    // @Transactional(rollbackFor = Exception.class)
    // public void delegateTask(WfTaskBo bo) {
    // // 当前任务 task
    // Task task =
    // taskService.createTaskQuery().taskId(bo.getTaskId()).singleResult();
    // if (ObjectUtil.isEmpty(task)) {
    // throw new ServiceException("获取任务失败！");
    // }
    // StringBuilder commentBuilder = new StringBuilder(LoginHelper.getNickName())
    // .append("->");
    // String nickName = sysUserService.selectNickNameById(bo.getUserId());
    // if (StringUtils.isNotBlank(nickName)) {
    // commentBuilder.append(nickName);
    // } else {
    // commentBuilder.append(bo.getUserId());
    // }
    // if (StringUtils.isNotBlank(bo.getComment())) {
    // commentBuilder.append(": ").append(bo.getComment());
    // }
    // identityService.setAuthenticatedUserId(TaskUtils.getUserId());
    // // 添加审批意见
    // taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(),
    // FlowComment.DELEGATE.getType(),
    // commentBuilder.toString());
    // // 设置办理人为当前登录人
    // taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
    // // 执行委派
    // taskService.delegateTask(bo.getTaskId(), bo.getUserId());
    // // 设置任务节点名称
    // bo.setTaskName(task.getName());
    // // 处理抄送用户
    // if (!copyService.makeCopy(bo)) {
    // throw new RuntimeException("抄送任务失败");
    // }
    // }

    /**
     * 委派任务
     *
     * @param bo 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(WfTaskBo bo) {
        String token = AuthorizationInterceptor.getToken();
        SysUser sysUser = getSysUser(token);
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException("获取任务失败！");
        }

        StringBuilder commentBuilder = new StringBuilder(sysUser.getUserName())
                .append("->");
        String userName = sysUserService.selectUserNameById(bo.getUserId());
        if (StringUtils.isNotBlank(userName)) {
            commentBuilder.append(userName);
        } else {
            commentBuilder.append(bo.getUserId());
        }
        if (StringUtils.isNotBlank(bo.getComment())) {
            commentBuilder.append(": ").append(bo.getComment());
        }
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // 添加审批意见
        taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(), FlowComment.DELEGATE.getType(),
                commentBuilder.toString());
        // 设置办理人为当前登录人
        taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
        // 执行委派
        taskService.delegateTask(bo.getTaskId(), bo.getUserId());
        // 设置任务节点名称
        bo.setTaskName(task.getName());
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
        // sendMessage(notifyAllSteps, bo.getUserId());
        // 调用统一消息推送方法
//        buildAndSendUnifiedMessage(task, bo.getUserId(), notifyAllSteps, sysUser);
        // 处理抄送用户
        bo.setPushMessage(notifyAllSteps);
        if (!copyService.makeCopy(bo, sysUser)) {
            throw new RuntimeException("抄送任务失败");
        }
    }

    /**
     * 转办任务
     *
     * @param bo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(WfTaskBo bo) {
        String token = AuthorizationInterceptor.getToken();
        SysUser sysUser = getSysUser(token);
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException("获取任务失败！");
        }
        StringBuilder commentBuilder = new StringBuilder(sysUser.getUserName())
                .append("->");
        String userName = sysUserService.selectUserNameById(bo.getUserId());
        if (StringUtils.isNotBlank(userName)) {
            commentBuilder.append(userName);
        } else {
            commentBuilder.append(bo.getUserId());
        }
        if (StringUtils.isNotBlank(bo.getComment())) {
            commentBuilder.append(": ").append(bo.getComment());
        }
        // 1. 更新工单状态
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // 添加审批意见
        taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(), FlowComment.TRANSFER.getType(),
                commentBuilder.toString());
        // 设置拥有者为当前登录人
        taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
        // 转办任务
        taskService.setAssignee(bo.getTaskId(), bo.getUserId());
        wfWorkOrderService.updateWorkOrderToPending(task, WorkOrderStatus.REFERRED.getStatus(), sysUser.getUserId());
        // 设置任务节点名称
        bo.setTaskName(task.getName());
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
        // sendMessage(notifyAllSteps, bo.getUserId());
        bo.setPushMessage(notifyAllSteps);

        // 调用统一消息推送方法
//        buildAndSendUnifiedMessage(task, bo.getUserId(), notifyAllSteps, sysUser);
        // 处理抄送用户
        if (!copyService.makeCopy(bo, sysUser)) {
            throw new RuntimeException("抄送任务失败");
        }
    }
    //
    // /**
    // * 转办任务
    // *
    // * @param bo 请求实体参数
    // */
    // @Override
    // @Transactional(rollbackFor = Exception.class)
    // public void transferTask(WfTaskBo bo) {
    // // 当前任务 task
    // Task task =
    // taskService.createTaskQuery().taskId(bo.getTaskId()).singleResult();
    // if (ObjectUtil.isEmpty(task)) {
    // throw new ServiceException("获取任务失败！");
    // }
    // StringBuilder commentBuilder = new StringBuilder(LoginHelper.getNickName())
    // .append("->");
    // String nickName =
    // sysUserService.selectNickNameById(Long.parseLong(bo.getUserId()));
    // if (StringUtils.isNotBlank(nickName)) {
    // commentBuilder.append(nickName);
    // } else {
    // commentBuilder.append(bo.getUserId());
    // }
    // if (StringUtils.isNotBlank(bo.getComment())) {
    // commentBuilder.append(": ").append(bo.getComment());
    // }
    // identityService.setAuthenticatedUserId(TaskUtils.getUserId());
    // // 添加审批意见
    // taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(),
    // FlowComment.TRANSFER.getType(),
    // commentBuilder.toString());
    // // 设置拥有者为当前登录人
    // taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
    // // 转办任务
    // taskService.setAssignee(bo.getTaskId(), bo.getUserId());
    // // 设置任务节点名称
    // bo.setTaskName(task.getName());
    // // 处理抄送用户
    // if (!copyService.makeCopy(bo)) {
    // throw new RuntimeException("抄送任务失败");
    // }
    // }

    /**
     * 取消申请
     *
     * @param bo
     * @return
     */
    @Override
    public void stopProcess(WfTaskBo bo) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        List<Task> taskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(bo.getProcInsId()).list();
        if (CollectionUtils.isEmpty(taskList)) {
            throw new RuntimeException("流程未启动或已执行完成，取消申请失败");
        }

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceId(bo.getProcInsId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        if (Objects.nonNull(bpmnModel)) {
            Process process = bpmnModel.getMainProcess();
            List<EndEvent> endNodes = process.findFlowElementsOfType(EndEvent.class, false);
            if (CollectionUtils.isNotEmpty(endNodes)) {
                Authentication.setAuthenticatedUserId(TaskUtils.getUserId());
                // taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
                // FlowComment.STOP
                // .getType(),
                // StringUtils.isBlank(flowTaskVo.getComment()) ? "取消申请" :
                // flowTaskVo.getComment());
                runtimeService.setVariable(processInstance.getId(), ProcessConstants.PROCESS_STATUS_KEY,
                        ProcessStatus.CANCELED.getStatus());
                for (Task task : taskList) {
                    taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
                            FlowComment.STOP.getType(), "取消流程");
                }
                // 1. 更新工单状态
                wfWorkOrderService.updateWorkOrderToPending(
                        taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                                .processInstanceId(bo.getProcInsId()).singleResult(),
                        WorkOrderStatus.CLOSED.getStatus(), null);
                // 获取当前流程最后一个节点
                String endId = endNodes.get(0).getId();
                List<Execution> executions = runtimeService.createExecutionQuery()
                        .parentId(processInstance.getProcessInstanceId()).list();
                List<String> executionIds = new ArrayList<>();
                executions.forEach(execution -> executionIds.add(execution.getId()));
                // 变更流程为已结束状态
                runtimeService.createChangeActivityStateBuilder()
                        .moveExecutionsToSingleActivityId(executionIds, endId).changeState();
            }
        }
    }

    /**
     * 撤回流程
     *
     * @param taskBo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeProcess(WfTaskBo taskBo) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        String procInsId = taskBo.getProcInsId();
        String taskId = taskBo.getTaskId();
        // 校验流程是否结束
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceId(procInsId)
                .active()
                .singleResult();
        if (ObjectUtil.isNull(processInstance)) {
            throw new RuntimeException("流程已结束或已挂起，无法执行撤回操作");
        }
        // 获取待撤回任务实例
        HistoricTaskInstance currTaskIns = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .taskAssignee(TaskUtils.getUserId())
                .singleResult();
        if (ObjectUtil.isNull(currTaskIns)) {
            throw new RuntimeException("当前任务不存在，无法执行撤回操作");
        }
        // 获取 bpmn 模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(currTaskIns.getProcessDefinitionId());
        UserTask currUserTask = ModelUtils.getUserTaskByKey(bpmnModel, currTaskIns.getTaskDefinitionKey());
        // 查找下一级用户任务列表
        List<UserTask> nextUserTaskList = ModelUtils.findNextUserTasks(currUserTask);
        List<String> nextUserTaskKeys = nextUserTaskList.stream().map(UserTask::getId).collect(Collectors.toList());

        // 获取当前节点之后已完成的流程历史节点
        List<HistoricTaskInstance> finishedTaskInsList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(procInsId)
                .taskCreatedAfter(currTaskIns.getEndTime())
                .finished()
                .list();
        for (HistoricTaskInstance finishedTaskInstance : finishedTaskInsList) {
            // 检查已完成流程历史节点是否存在下一级中
            if (CollUtil.contains(nextUserTaskKeys, finishedTaskInstance.getTaskDefinitionKey())) {
                throw new RuntimeException("下一流程已处理，无法执行撤回操作");
            }
        }
        // 获取所有激活的任务节点，找到需要撤回的任务
        List<Task> activateTaskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(procInsId).list();
        List<String> revokeExecutionIds = new ArrayList<>();
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        for (Task task : activateTaskList) {
            // 检查激活的任务节点是否存在下一级中，如果存在，则加入到需要撤回的节点
            if (CollUtil.contains(nextUserTaskKeys, task.getTaskDefinitionKey())) {
                // 添加撤回审批信息
                taskService.setAssignee(task.getId(), TaskUtils.getUserId());
                taskService.addComment(task.getId(), task.getProcessInstanceId(), FlowComment.REVOKE.getType(),
                        LoginHelper.getUsername() + "撤回流程审批");
                revokeExecutionIds.add(task.getExecutionId());
            }
        }
        try {
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(procInsId)
                    .moveExecutionsToSingleActivityId(revokeExecutionIds, currTaskIns.getTaskDefinitionKey())
                    .changeState();
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new RuntimeException("执行撤回操作失败");
        }
    }

    /**
     * 获取流程过程图
     *
     * @param processId
     * @return
     */
    @Override
    public InputStream diagram(String processId) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        String processDefinitionId;
        // 获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(processId).singleResult();
        // 如果流程已经结束，则得到结束节点
        if (Objects.isNull(processInstance)) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId())
                    .processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        } else {// 如果流程没有结束，则取当前活动节点
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }

        // 获得活动的节点
        List<HistoricActivityInstance> highLightedFlowList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();

        List<String> highLightedFlows = new ArrayList<>();
        List<String> highLightedNodes = new ArrayList<>();
        // 高亮线
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            if ("sequenceFlow".equals(tempActivity.getActivityType())) {
                // 高亮线
                highLightedFlows.add(tempActivity.getActivityId());
            } else {
                // 高亮节点
                highLightedNodes.add(tempActivity.getActivityId());
            }
        }

        // 获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
        // 获取自定义图片生成器
        ProcessDiagramGenerator diagramGenerator = new CustomProcessDiagramGenerator();
        return diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodes, highLightedFlows,
                configuration.getActivityFontName(),
                configuration.getLabelFontName(), configuration.getAnnotationFontName(), configuration.getClassLoader(),
                1.0, true);

    }

    /**
     * 获取流程变量
     *
     * @param taskId 任务ID
     * @return 流程变量
     */
    @Override
    public Map<String, Object> getProcessVariables(String taskId) {
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables()
                .finished()
                .taskId(taskId)
                .singleResult();
        if (Objects.nonNull(historicTaskInstance)) {
            return historicTaskInstance.getProcessVariables();
        }
        return taskService.getVariables(taskId);
    }

    // /**
    // * 启动第一个任务
    // *
    // * @param processInstance 流程实例
    // * @param variables 流程参数
    // */
    // @Override
    // public void startFirstTask(ProcessInstance processInstance, Map<String,
    // Object> variables) {
    // // 若第一个用户任务为发起人，则自动完成任务
    // List<Task> tasks = taskService.createTaskQuery().
    // taskTenantId(sysUser.getTenantId()).
    // processInstanceId(processInstance.getProcessInstanceId()).list();
    // if (CollUtil.isNotEmpty(tasks)) {
    // String userIdStr = (String) variables.get(TaskConstants.PROCESS_INITIATOR);
    // identityService.setAuthenticatedUserId(TaskUtils.getUserId());
    // for (Task task : tasks) {
    // if (StrUtil.equals(task.getAssignee(), userIdStr)) {
    // taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
    // FlowComment.NORMAL
    // .getType(), LoginHelper.getNickName() + "发起流程申请");
    // taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
    // FlowComment.NORMAL
    // .getType(), LoginHelper.getNickName() + "发起流程申请");
    // taskService.complete(task.getId(), variables);
    // }
    // }
    // }
    // }

    /**
     * 启动第一个任务
     *
     * @param processInstance 流程实例
     * @param variables       流程参数
     */
    @Override
    public void startFirstTask(ProcessInstance processInstance, Map<String, Object> variables, SysUser sysUser) {
        // 若第一个用户任务为发起人，则自动完成任务
        List<Task> tasks = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInstance.getProcessInstanceId()).list();
        if (CollUtil.isNotEmpty(tasks)) {
            String userIdStr = (String) variables.get(TaskConstants.PROCESS_INITIATOR);
            identityService.setAuthenticatedUserId(sysUser.getUserId());
            for (Task task : tasks) {
                BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
                log.info("==========model:{}", ModelUtils.getBpmnXmlStr(model));
                // 判断当前节点的审批人是否是发起人
                boolean a = sysUser.getUserId().equals(task.getAssignee());
                if (StrUtil.equals(task.getAssignee(), userIdStr)) {
                    if (sysUser != null) {
                        taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
                                FlowComment.NORMAL.getType(), sysUser.getUserName() + "发起流程申请");
                    }
                    taskService.complete(task.getId(), variables);
                    // 完成后立刻查询
                    List<Task> nextTasks = taskService.createTaskQuery()
                            .processInstanceId(processInstance.getId())
                            .active()
                            .list();
                    System.out.println("complete 后 active 任务数量：" + nextTasks.size());
                    nextTasks.forEach(t -> System.out
                            .println("taskId=" + t.getId() + ", taskDefKey=" + t.getTaskDefinitionKey()));

                    // 获取最新节点
                    List<Task> newTaskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                            .processInstanceId(processInstance.getId()).active().list();
                    // 当前节点的审批人是发起人，则将页面上指定的审批人传入新的节点
                    newTaskList.stream().forEach(newTask -> {
                        if (a) {
                            String nextUserIds = (String) variables.get("nextUserIds");
                            if (StringUtils.isNotBlank(nextUserIds)) {
                                String[] userIdArray = nextUserIds.split(",");
                                List<String> candidateUserIds = Arrays.asList(userIdArray);
                                for (String userId : candidateUserIds) {
                                    // 置空审批人，防止待签查不到流程
                                    taskService.setAssignee(newTask.getId(), null);
                                    taskService.addCandidateUser(newTask.getId(), userId);
                                }
                            }
                            // 腾讯需要，设置办理人
                            String nextAssignees = (String) variables.get("nextAssignee");
                            if (StringUtils.isNotBlank(nextAssignees)) {
                                String[] userIdArray = nextAssignees.split(",");
                                List<String> assigneeUserIds = Arrays.asList(userIdArray);
                                for (String userId : assigneeUserIds) {
                                    taskService.setAssignee(newTask.getId(), userId);
                                }
                            }
                        }
                        boolean notifyAllSteps = (boolean) runtimeService.getVariable(newTask.getExecutionId(),
                                "notifyAllSteps");
                        // boolean flag = variables.get("isPushNotification") != null && (boolean)
                        // variables.get
                        // ("isPushNotification");
                        // sendMessage(notifyAllSteps, newtask.getAssignee());
                        // 调用统一消息推送方法
//                        buildAndSendUnifiedMessage(newTask, newTask.getAssignee(), notifyAllSteps, sysUser);
                    });
                }
            }
        }
    }

    /**
     * 指派下一任务审批人或设置候选审批人
     *
     * @param bpmnModel      BPMN模型
     * @param processInsId   流程实例ID
     * @param userIds        直接指派的用户ID列表（逗号分隔）
     * @param candidateUsers 候选用户ID列表（若不为空，则优先设置候选用户）
     * @param isPushMessage  是否发送通知
     * @param sysUser        当前用户（用于租户隔离）
     */
    private void assignORCandidateUserNextUsers(BpmnModel bpmnModel, String processInsId, String userIds,
            List<String> candidateUsers, List<String> candidateGroups,
            boolean isPushMessage, SysUser sysUser) {
        // 1. 查询当前任务列表
        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInsId)
                .list();
        if (CollUtil.isEmpty(tasks)) {
            return;
        }

        // 2. 处理候选用户逻辑（优先级高于直接指派）
        if (CollUtil.isNotEmpty(candidateUsers)) {
            handleCandidateUsers(bpmnModel, tasks, candidateUsers);
        } else if (CollUtil.isNotEmpty(candidateGroups)) {
            CandidateNextGroups(bpmnModel, processInsId, candidateGroups, sysUser);
        } else {
            handleDirectAssignment(bpmnModel, tasks, userIds);
        }

        // if (CollUtil.isEmpty(candidateGroups)) {
        // // 3. 发送通知
        // Task latestTask = taskService.createTaskQuery()
        // .taskTenantId(sysUser.getTenantId())
        // .processInstanceId(processInsId)
        // .orderByTaskCreateTime().desc()
        // .singleResult();
        // buildAndSendUnifiedMessage(latestTask, String.join(",", candidateUsers),
        // isPushMessage);
        // }
    }

    /**
     * 处理候选用户设置
     */
    private void handleCandidateUsers(BpmnModel bpmnModel, List<Task> tasks, List<String> candidateUsers) {
        Iterator<Task> iterator = tasks.iterator();
        // 1. 更新工单状态为待接单
        wfWorkOrderService.updateWorkOrderToPending(tasks.get(0), WorkOrderStatus.PENDING_ORDERS.getStatus(), null);

        while (iterator.hasNext()) {
            Task task = iterator.next();
            String taskDefKey = task.getTaskDefinitionKey();

            // 非多实例任务：直接添加候选用户
            if (!ModelUtils.isMultiInstance(bpmnModel, taskDefKey)) {
                taskService.setAssignee(task.getId(), null);
                candidateUsers.forEach(userId -> taskService.addCandidateUser(task.getId(), userId));
                iterator.remove();
            }
        }

        // 多实例任务：批量添加候选用户实例
        if (CollUtil.isNotEmpty(tasks)) {
            tasks.forEach(task -> {
                Map<String, Object> variables = new HashMap<>();
                variables.put(ProcessConstants.USER_TYPE_USERS, candidateUsers); // 批量传递用户集合
                runtimeService.addMultiInstanceExecution(
                        task.getTaskDefinitionKey(),
                        task.getProcessInstanceId(),
                        variables);
            });
        }
    }

    /**
     * 处理直接指派逻辑（原逻辑改造）
     */
    private void handleDirectAssignment(BpmnModel bpmnModel, List<Task> tasks, String userIds) {
        Queue<String> assignIds = CollUtil.newLinkedList(userIds.split(","));
        if (tasks.size() == assignIds.size()) {
            tasks.forEach(task -> taskService.setAssignee(task.getId(), assignIds.poll()));
            return;
        }
        // 1. 更新工单状态为处理中
        wfWorkOrderService.updateWorkOrderToPending(tasks.get(0), WorkOrderStatus.PROCESSING.getStatus(), null);

        Iterator<Task> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (!ModelUtils.isMultiInstance(bpmnModel, task.getTaskDefinitionKey())) {
                if (!assignIds.isEmpty()) {
                    taskService.setAssignee(task.getId(), assignIds.poll());
                }
                iterator.remove();
            }
        }

        if (CollUtil.isNotEmpty(tasks)) {
            if (assignIds.isEmpty()) {
                tasks.forEach(task -> runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), true));
            } else {
                assignIds.forEach(userId -> {
                    Map<String, Object> variables = Collections.singletonMap(
                            BpmnXMLConstants.ATTRIBUTE_TASK_USER_ASSIGNEE, userId);
                    runtimeService.addMultiInstanceExecution(
                            tasks.get(0).getTaskDefinitionKey(),
                            tasks.get(0).getProcessInstanceId(),
                            variables);
                });
            }
        }
    }

    /**
     * 设置下一任务的候选审批组（部门）
     *
     * @param bpmnModel       BPMN模型
     * @param processInsId    流程实例ID
     * @param candidateGroups 候选部门ID列表（逗号分隔）
     * @param sysUser         当前用户（用于租户隔离）
     */
    private void CandidateNextGroups(BpmnModel bpmnModel, String processInsId, List<String> candidateGroups, SysUser sysUser) {
        // 1. 查询当前流程实例的所有任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInsId)
                .list();
        // 1. 更新工单状态为待接单
        wfWorkOrderService.updateWorkOrderToPending(tasks.get(0), WorkOrderStatus.PENDING_ORDERS.getStatus(), null);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        if (CollUtil.isEmpty(candidateGroups)) {
            throw new RuntimeException("候选部门ID不能为空");
        }
        // 3. 遍历任务，设置候选组（优先处理非多实例任务）
        Iterator<Task> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            String taskDefKey = task.getTaskDefinitionKey();

            // 3.1 非多实例任务：直接设置候选组
            if (!ModelUtils.isMultiInstance(bpmnModel, taskDefKey)) {
                taskService.setAssignee(task.getId(), null);
                for (String groupId : candidateGroups) {
                    taskService.addCandidateGroup(task.getId(), groupId);
                }
                iterator.remove();
            }
        }

        // 4. 处理多实例任务（动态调整候选组）
        if (CollUtil.isNotEmpty(tasks)) {
            tasks.forEach(task -> {
                Map<String, Object> variables = new HashMap<>();
                variables.put(ProcessConstants.USER_TYPE_ROUPS, candidateGroups); // 传递完整集合
                runtimeService.addMultiInstanceExecution(
                        task.getTaskDefinitionKey(), // 遍历所有任务
                        task.getProcessInstanceId(),
                        variables);
            });
        }
    }

    /**
     * 保存流程pdf1
     *
     * @param bo
     */
    @Override
    public void savePDF(WfSavePdfBo bo) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        WfAttachment wfAttachment = new WfAttachment();
        if (bo.getId() != null) {
            wfAttachment = wfAttachmentMapper.selectById(bo.getId());
        } else {
            wfAttachment.setCreateBy(sysUser.getUserId());
        }
        wfAttachment.setAttachmentLink(bo.getAttachmentLink());
        wfAttachment.setIsSignature(bo.getIsSignature());
        wfAttachment.setTaskId(bo.getTaskId());
        wfAttachment.setProcInsId(bo.getProcInsId());
        wfAttachment.setUpdateBy(sysUser.getUserId());
        wfAttachmentMapper.insertOrUpdate(wfAttachment);
    }

    /**
     * 获取流程最新节点的审批人信息
     *
     * @param processInstanceId
     * @return
     */
    @Override
    public List<String> getApproverIds(String processInstanceId) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        List<String> approvers = new ArrayList<>();
        // 1. 获取流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            // 2. 获取当前任务
            Task currentTask = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (currentTask != null) {
                // 3. 获取当前任务的审批人信息
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                for (IdentityLink identityLink : identityLinks) {
                    if (IdentityLinkType.CANDIDATE.equals(identityLink.getType())) {
                        if (StringUtils.isNotBlank(identityLink.getUserId())) {
                            approvers.add(identityLink.getUserId()); // 获取审批人的用户ID
                        } else if (StringUtils.isNotBlank(identityLink.getGroupId())
                                && identityLink.getGroupId().startsWith("ROLE")) {
                            int startIndex = "ROLE".length();
                            Long roleId = Long.valueOf(identityLink.getGroupId().substring(startIndex));
                            // 获取role下所有人
                            List<String> s = sysUserRoleViewMapper.selectUserIdsByRoleId(roleId);
                            approvers.addAll(s);
                        } else if (StringUtils.isNotBlank(identityLink.getGroupId())
                                && identityLink.getGroupId().startsWith("DEPT")) {
                            int startIndex = "DEPT".length();
                            String deptId = identityLink.getGroupId().substring(startIndex);
                            List<SysUser> s = sysUserMapper.selectLeadersByDeptId(deptId);
                            List<String> collect = s.stream().map(SysUser::getUserId).collect(Collectors.toList());
                            approvers.addAll(collect);
                        }
                    }
                }
            } else {
                throw new RuntimeException("No task found for process instance: " + processInstanceId);
            }
        } else {
            throw new RuntimeException("Process instance not found: " + processInstanceId);
        }
        return approvers;
    }

    /**
     * 通过token获取用户信息
     *
     * @param token
     * @return
     */
    private SysUser getSysUser(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if (user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        System.out.println(" 用户缓存信息 " + user);
        return user;
    }

    /**
     * 获取任务节点的 noNotifyAllSteps 配置
     * 从任务节点的扩展属性中读取配置，判断是否需要发送消息通知
     *
     * @param task 任务对象
     * @return 如果配置了不通知返回 true，否则返回 false
     */
    private Boolean getNoNotifyAllSteps(Task task) {
        // 获取流程定义模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        // 获取当前任务节点
        FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());

        if (flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;

            // 从扩展属性中读取 noNotifyAllSteps
            Map<String, List<ExtensionAttribute>> attributes = userTask.getAttributes();
            if (attributes != null && attributes.containsKey("http://flowable.org/bpmn")) {
                List<ExtensionAttribute> bpmnAttributes = attributes.get("http://flowable.org/bpmn");
                for (ExtensionAttribute attr : bpmnAttributes) {
                    if ("flowable:noNotifyAllSteps".equals(attr.getName())) {
                        return "true".equalsIgnoreCase(attr.getValue());
                    }
                }
            }
        }

        // 默认返回 false（需要通知）
        return false;
    }

    /**
     * 获取流程名称
     *
     * @param historicProcessInstance 流程实例
     * @param sysUser                 当前用户
     * @return 流程名称
     */
    public String getProcessName(HistoricProcessInstance historicProcessInstance, SysUser sysUser) {
        return repositoryService.createDeploymentQuery()
                .deploymentId(historicProcessInstance.getDeploymentId())
                .deploymentTenantId(sysUser.getTenantId())
                .singleResult()
                .getName();
    }

    /**
     * 根据流程定义ID获取流程定义中存储的标识变量
     *
     * @param processDefinitionId 流程定义ID
     */
    public Map<String, String> getProcessDefinitionProperties(String processDefinitionId) {
        // 1. 获取流程定义的扩展元素信息
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        // 2. 通过 BPMNModel 获取扩展属性
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        // 获取流程定义（Process）对象
        org.flowable.bpmn.model.Process process = bpmnModel.getProcessById(processDefinition.getKey());

        Map<String, String> properties = new HashMap<>();
        if (process != null) {
            // 获取所有 flowable:property 扩展元素
            List<ExtensionElement> extensionElements = process.getExtensionElements().get("property");

            if (extensionElements != null) {
                for (ExtensionElement element : extensionElements) {
                    String name = element.getAttributeValue(null, "name");
                    String value = element.getAttributeValue(null, "value");
                    if (name != null && value != null) {
                        properties.put(name, value);
                    }
                }
            }
        }
        return properties;
    }

    /**
     * 构建并发送统一消息
     *
     * @param task           当前任务
     * @param receive_userId 目标id
     * @param notifyAllSteps 是否推送消息
     */
    public void buildAndSendUnifiedMessage(Task task, String receive_userId, boolean notifyAllSteps, SysUser sysUser) {
        // 从任务节点的扩展属性中获取 noNotifyAllSteps 配置
        Boolean noNotifyAllSteps = getNoNotifyAllSteps(task);
        if (noNotifyAllSteps) {
            log.warn("任务节点配置了 noNotifyAllSteps，无需推送信息");
            return;
        }

        if (StringUtils.isBlank(receive_userId)) {
            log.warn("下一节点无审批人，无推送目标");
            return;
        }

        // 查询流程实例及其变量
        HistoricProcessInstance historicProcessInstance =
            wfInstanceService.getHistoricProcessInstanceById(task.getProcessInstanceId(), sysUser);
        Map<String, Object> processVariables = runtimeService.getVariables(historicProcessInstance.getId());
        String applicationId = Optional.ofNullable(processVariables.get("appid")).map(Objects::toString).orElse(null);
        if (StringUtils.isBlank(applicationId)) {
            log.error("没找到应用ID");
            return;
        }
        // 获取流程变量
        String receiveUserName = sysUserService.selectUserNameById(receive_userId);

        // 获取流程名称
        String processName = getProcessName(historicProcessInstance, sysUser);

        // 构建 content 值
        String content = buildContent(processName, processVariables, receiveUserName);
        OkHttpClient client = OkHttpClientHolder.CLIENT;
        HttpUrl.Builder urlBuilder = HttpUrl.get(msgUrl + "app/v1/relatesinfo").newBuilder();
        urlBuilder.addQueryParameter("data_id", applicationId);
        Request.Builder url = new Request.Builder().url(urlBuilder.build()).get();
        ApiHeaderUtil.transferHeaders(url);
        Request request = url.build();
        log.info("请求头："+request.headers().toString());
        String ductCode = null;
        String channelCode = null;
        String variableValue = null;
        String templateCode = null;
        log.info("调用消息接口："+urlBuilder.toString());
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("获取通道编码和渠道编码失败，HTTP 状态码：{}", response.code());
                return;
            }

            String jsonData = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonData);

            // 定位到 data.list 数组
            JsonNode listNode = root.path("data").path("list");
            if (!listNode.isArray()) {
                log.error("返回结构里找不到 data.list"+root.toString());
                return;
            }
            // 遍历 list 数组
            for (JsonNode item : listNode) {
                // 拿到子节点 channel_list
                JsonNode channelList = item.path("channel_list");

                // 只有当 channel_list 存在且是数组，且至少有一个元素时，才取第一个
                if (channelList.isArray() && channelList.size() > 0) {
                    for (JsonNode jsonNode : channelList) {
                        if (jsonNode.path("channel_types").asText().equals("1")) {
                            // 取当前项的 duct_code
                            ductCode = item.path("duct_code").asText();
                            // 取 channel_list 下第一个元素的 channel_code
                            channelCode = jsonNode.path("channel_code").asText();
                            break; // 找到第一个就退出循环
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("请求或解析返回数据失败", e);
        }
        // 构建推送参数
        Map<String, Object> params = ApiHeaderUtil.buildParams(ductCode, // 通道编码
                channelCode, // 渠道编码
                receive_userId, // 接收用户ID
                receiveUserName, // 接收用户名称
                processName, // 消息标题
                content, // 消息内容
                "1", // 消息类型
                task.getProcessInstanceId(), // 流程实例ID
                task.getId(), // 任务ID
                templateCode, variableValue);

        // 调用统一消息推送方法
        unifiedMessageSend(notifyAllSteps, params);
    }

    @Override
    public void addSignTask(WfTaskBo bo) {
        runtimeService.addMultiInstanceExecution(bo.getTargetKey(), bo.getProcInsId(), bo.getVariables());
    }

    @Override
    public void subSignTask(WfTaskBo bo) {
        Task task = taskService.createTaskQuery().taskId(bo.getTaskId()).singleResult();
        String executionId = task.getExecutionId();
        runtimeService.deleteMultiInstanceExecution(executionId, false);
    }

}
