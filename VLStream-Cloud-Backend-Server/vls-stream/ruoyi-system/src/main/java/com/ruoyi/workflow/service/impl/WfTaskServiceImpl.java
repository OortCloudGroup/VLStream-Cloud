/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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

    @Value("${UnifiedMessagingSend.url}")
    private String msgUrl;


    // Push
    public void sendMessage(boolean isPushMessage, String userIds) {
        if (isPushMessage) {
            List<String> idCards = new ArrayList<>();
            String[] userIdArray = userIds.split(",");
            for (String id : userIdArray) {
                idCards.add(sysUserService.selectIdCardById(id));
            }
            if (!idCards.isEmpty()) {
//                sendNotification(idCards);
            }
        }
    }

    /**
     * Push
     *
     * @return
     */
    public void unifiedMessageSend(boolean isPushMessage, Map<String, Object> params) {
        if (isPushMessage) {
            OkHttpClient client = new OkHttpClient.Builder().build();

            // FastJSON Build
            String jsonBody = JSON.toJSONString(ApiHeaderUtil.createRequestBody(params));

            // MediaType and RequestBody
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, jsonBody);

            // Build
            Request.Builder requestBuilder = new Request.Builder().url(msgUrl + "msg/v1/send").post(body);

            // current all info, new in
            ApiHeaderUtil.transferHeaders(requestBuilder);

            Request request = requestBuilder.build();

            // Execute Process
            try (Response response = client.newCall(request).execute()) {
                System.out.println("Response Code: " + response.code());
                System.out.println("Response Body: " + response.body().string());
            } catch (Exception e) {
                throw new RuntimeException("Error executing HTTP request", e);
            }
        }
    }

    // /**
    // * task
    // *
    // * @param taskBo parameter
    // */
    // @Transactional(rollbackFor = Exception.class)
    // @Override
    // public void complete(WfTaskBo taskBo) {
    // Task task =
    // taskService.createTaskQuery().taskId(taskBo.getTaskId()).singleResult();
    // if (Objects.isNull(task)) {
    // throw new ServiceException("task in ");
    // }
    // // Get bpmn model
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
    // // Get modelinfo
    // String localScopeValue = ModelUtils.getUserTaskAttributeValue(bpmnModel,
    // task.getTaskDefinitionKey
    // (), ProcessConstants.PROCESS_FORM_LOCAL_SCOPE);
    // boolean localScope = Convert.toBool(localScopeValue, false);
    // taskService.complete(taskBo.getTaskId(), taskBo.getVariables(), localScope);
    // } else {
    // taskService.complete(taskBo.getTaskId());
    // }
    // }
    // // Set tasknode
    // taskBo.setTaskName(task.getName());
    // // Process approver
    // if (StringUtils.isNotBlank(taskBo.getNextUserIds())) {
    // this.assignORCandidateUserNextUsers(bpmnModel, taskBo.getProcInsId(),
    // taskBo.getNextUserIds(), taskBo
    // .isPushMessage());
    // }
    // // Process user
    // if (!copyService.makeCopy(taskBo)) {
    // throw new RuntimeException(" taskfailed");
    // }
    // }

    /**
     * task
     *
     * @param taskBo parameter
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
            // workflow already , operation
            throw new RuntimeException("流程实例已被挂起，无法继续操作");
        }
        // Get bpmn model
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        if (user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrder::getProcInsId, taskBo.getProcInsId());
        WorkOrder one = wfWorkOrderService.getOne(queryWrapper);
        runtimeService.setVariable(taskBo.getProcInsId(), "currentAssignee", user.getUserId());
        // Get whether need to Push
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
        if (ObjectUtil.isNotNull(runtimeService.getVariable(taskBo.getProcInsId(), "flowDirection"))) {
            if (StringUtils.isNotBlank(taskBo.getNextUserIds()) || ObjectUtil.isNotEmpty(taskBo.getCandidateUsers())
                    || ObjectUtil.isNotEmpty(taskBo.getCandidateGroups())) {
                runtimeService.setVariable(taskBo.getProcInsId(), "nextUserIds", taskBo.getNextUserIds());
            } else {
                // Check if it is bureau leader
                SysUser sysUser = sysUserMapper.selectUserById(user.getUserId());
                SysDeptView sysDeptView = sysDeptMapper.selectVoById(sysUser.getDeptId());
// if (sysDeptView.getDeptId().equals(excludedUdid)) { // bureau leader, finishworkflow
//                    runtimeService.setVariable(taskBo.getProcInsId(), "flowDirection", "end");
// } else { // bureau leader, Get all leader
                    List<SysUser> leaders = sysUserService.getLeaders(user.getUserId());
                    String join = leaders.stream().map(SysUser::getUserId).collect(Collectors.joining(","));
                    this.assignORCandidateUserNextUsers(bpmnModel, taskBo.getProcInsId(), join,
                            taskBo.getCandidateUsers(), taskBo.getCandidateGroups(), notifyAllSteps, user);
                    runtimeService.setVariable(taskBo.getProcInsId(), "nextUserIds", join);
//                }
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
                // Get modelinfo
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

        // Set tasknode
        taskBo.setTaskName(task.getName());
        // Process approver
        if (StringUtils.isNotBlank(taskBo.getNextUserIds()) || ObjectUtil.isNotEmpty(taskBo.getCandidateUsers())
                || ObjectUtil.isNotEmpty(taskBo.getCandidateGroups())) { // approver
            // user in after node Set node approver
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
     * task
     *
     * @param taskBo
     * @param user
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void taskReject(WfTaskBo taskBo, SysUser user) {
        // current task task
        Task task = taskService.createTaskQuery().taskTenantId(user.getTenantId()).taskId(taskBo.getTaskId())
                .singleResult();
        if (ObjectUtil.isNull(task)) {
            throw new RuntimeException("获取任务信息异常！");
        }
        if (task.isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // Get workflow instance
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(user.getTenantId()).processInstanceId(taskBo.getProcInsId()).singleResult();
        if (processInstance == null) {
            throw new RuntimeException("流程实例不存在，请确认！");
        }
        // Get workflow definitioninfo
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(user.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                .singleResult();

        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // approval
        taskService.addComment(taskBo.getTaskId(), taskBo.getProcInsId(), FlowComment.REJECT.getType(),
                taskBo.getComment());
        // Set workflow to already
        runtimeService.setVariable(processInstance.getId(), ProcessConstants.PROCESS_STATUS_KEY,
                ProcessStatus.TERMINATED.getStatus());
        // node Process info workflow variable in
        runtimeService.setVariable(taskBo.getProcInsId(), "rejectAssignee", user.getUserId());
        runtimeService.setVariable(taskBo.getProcInsId(), "rejectTaskId", task.getId());
        runtimeService.setVariable(taskBo.getProcInsId(), "rejectTaskName", task.getName());
        // Get all nodeinfo
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        EndEvent endEvent = ModelUtils.getEndEvent(bpmnModel);
        // Get user ID
        String initiatorId = (String) runtimeService.getVariable(processInstance.getId(), "initiator");
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getProcessInstanceId(), "notifyAllSteps");
        // sendMessage(notifyAllSteps, initiatorId);

        // Push method
//        buildAndSendUnifiedMessage(task, initiatorId, notifyAllSteps, user);

        // workflow
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(task.getProcessInstanceId()).list();
        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());
        runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                .moveExecutionsToSingleActivityId(executionIds, endEvent.getId()).changeState();
        // Process user
        taskBo.setPushMessage(notifyAllSteps);
        if (!copyService.makeCopy(taskBo, user)) {
            throw new RuntimeException("抄送任务失败");
        }
    }

    /**
     * Build content value
     *
     * @param processName workflow
     * @param processVariables workflow variable
     * @return Generate content value
     */
    private String buildContent(String processName, Map<String, Object> processVariables, String receiveUserName) {
        // Get workflow form item ID
        Map<String, String[]> processVariableIds = UnifiedMessageUtil.getProcessVariableIds();

        // Check whether is workflow
        if (processVariableIds.containsKey(processName)) {
            // Get current workflow form item ID
            String[] variableIds = processVariableIds.get(processName);
            String typeVariableId = variableIds[0];
            String nameVariableId = variableIds[1];

            // Get and
            String type = (String) processVariables.getOrDefault(typeVariableId, "默认类型");
            String name = (String) processVariables.getOrDefault(nameVariableId, "默认名称");

            // content value
            return type + name + processName;
        } else {
            // non- workflow, content value to workflow
            return receiveUserName + processName;
        }
    }

    /**
     * task
     *
     * @param bo parameter
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void taskReturn(WfTaskBo bo, SysUser sysUser) {
        // current task task
        Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        if (ObjectUtil.isNull(task)) {
            throw new RuntimeException("获取任务信息异常！");
        }
        if (task.isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // Get workflow definitioninfo
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        // Get workflowmodelinfo
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // Get current tasknodeelement
        FlowElement source = ModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        // Get nodeelement
        FlowElement target = ModelUtils.getFlowElementById(bpmnModel, bo.getTargetKey());
        // from current node before , Check current node and nodewhether , node is in non- ,
        boolean isSequential = ModelUtils.isSequentialReachable(source, target, new HashSet<>());
        if (!isSequential) {
            throw new RuntimeException("当前节点相对于目标节点，不属于串行关系，无法回退");
        }

        // Get all tasknode Key, task can , need to in need to task
        List<Task> runTaskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // task
        List<String> currentIds = new ArrayList<>();
        // , runTaskList , Get need to task
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null,
                null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

        // loopGet need to node ID, Set
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // Set
        for (String currentTaskId : currentTaskIds) {
            taskService.addComment(currentTaskId, task.getProcessInstanceId(), FlowComment.REBACK.getType(),
                    bo.getComment());
        }

        try {
            // 1 1 1 , currentIds current need to node (1 ), targetKey node(1)
            runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentIds, bo.getTargetKey()).changeState();
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new RuntimeException("无法取消或开始活动");
        }
        // Set tasknode
        bo.setTaskName(task.getName());
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getProcessInstanceId(), "notifyAllSteps");
        bo.setPushMessage(notifyAllSteps);
        // Process user
        if (!copyService.makeCopy(bo, sysUser)) {
            throw new RuntimeException("抄送任务失败");
        }
        // Get node taskassigneeID
        Task targetTask = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(task.getProcessInstanceId()).taskDefinitionKey(bo.getTargetKey()).singleResult();
        // 1. new work order
        wfWorkOrderService.updateWorkOrderToPending(targetTask, WorkOrderStatus.RETURNED.getStatus(), null);
        if (targetTask != null) {
            String targetAssignee = targetTask.getAssignee();
            // sendMessage(notifyAllSteps, targetAssignee);

            // Push method
//            buildAndSendUnifiedMessage(task, targetAssignee, notifyAllSteps, sysUser);
        } else {
            throw new RuntimeException("未找到目标节点的任务");
        }
    }

    /**
     * Get all node
     *
     * @param bo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<FlowElement> findReturnTaskList(WfTaskBo bo) {
        SysUser user = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // current task task
        Task task = taskService.createTaskQuery().taskTenantId(user.getTenantId()).taskId(bo.getTaskId())
                .singleResult();
        // Get workflow definitioninfo
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(user.getTenantId())
                .processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // Get workflowmodelinfo
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // Query history nodeinstance
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
        // Get current tasknodeelement
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
     * Delete task
     *
     * @param bo parameter
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteTask(WfTaskBo bo) {
        // todo Delete task is Delete task is Delete , task ?
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        taskService.deleteTask(bo.getTaskId(), bo.getComment());
    }

    /**
     * / task
     *
     * @param taskBo parameter
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
     * / task
     *
     * @param bo parameter
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
    // * task
    // *
    // * @param bo parameter
    // */
    // @Override
    // @Transactional(rollbackFor = Exception.class)
    // public void delegateTask(WfTaskBo bo) {
    // // current task task
    // Task task =
    // taskService.createTaskQuery().taskId(bo.getTaskId()).singleResult();
    // if (ObjectUtil.isEmpty(task)) {
    // throw new ServiceException("Get taskfailed! ");
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
    // // approval
    // taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(),
    // FlowComment.DELEGATE.getType(),
    // commentBuilder.toString());
    // // Set assignee to current
    // taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
    // // Execute
    // taskService.delegateTask(bo.getTaskId(), bo.getUserId());
    // // Set tasknode
    // bo.setTaskName(task.getName());
    // // Process user
    // if (!copyService.makeCopy(bo)) {
    // throw new RuntimeException(" taskfailed");
    // }
    // }

    /**
     * task
     *
     * @param bo parameter
     */
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(WfTaskBo bo) {
        String token = AuthorizationInterceptor.getToken();
        SysUser sysUser = getSysUser(token);
        // current task task
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
        // approval
        taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(), FlowComment.DELEGATE.getType(),
                commentBuilder.toString());
        // Set assignee to current
        taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
        // Execute
        taskService.delegateTask(bo.getTaskId(), bo.getUserId());
        // Set tasknode
        bo.setTaskName(task.getName());
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
        // sendMessage(notifyAllSteps, bo.getUserId());
        // Push method
//        buildAndSendUnifiedMessage(task, bo.getUserId(), notifyAllSteps, sysUser);
        // Process user
        bo.setPushMessage(notifyAllSteps);
        if (!copyService.makeCopy(bo, sysUser)) {
            throw new RuntimeException("抄送任务失败");
        }
    }

    /**
     * task
     *
     * @param bo parameter
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(WfTaskBo bo) {
        String token = AuthorizationInterceptor.getToken();
        SysUser sysUser = getSysUser(token);
        // current task task
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
        // 1. new work order
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        // approval
        taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(), FlowComment.TRANSFER.getType(),
                commentBuilder.toString());
        // Set to current
        taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
        // task
        taskService.setAssignee(bo.getTaskId(), bo.getUserId());
        wfWorkOrderService.updateWorkOrderToPending(task, WorkOrderStatus.REFERRED.getStatus(), sysUser.getUserId());
        // Set tasknode
        bo.setTaskName(task.getName());
        boolean notifyAllSteps = (boolean) runtimeService.getVariable(task.getExecutionId(), "notifyAllSteps");
        // sendMessage(notifyAllSteps, bo.getUserId());
        bo.setPushMessage(notifyAllSteps);

        // Push method
//        buildAndSendUnifiedMessage(task, bo.getUserId(), notifyAllSteps, sysUser);
        // Process user
        if (!copyService.makeCopy(bo, sysUser)) {
            throw new RuntimeException("抄送任务失败");
        }
    }
    //
    // /**
    // * task
    // *
    // * @param bo parameter
    // */
    // @Override
    // @Transactional(rollbackFor = Exception.class)
    // public void transferTask(WfTaskBo bo) {
    // // current task task
    // Task task =
    // taskService.createTaskQuery().taskId(bo.getTaskId()).singleResult();
    // if (ObjectUtil.isEmpty(task)) {
    // throw new ServiceException("Get taskfailed! ");
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
    // // approval
    // taskService.addComment(bo.getTaskId(), task.getProcessInstanceId(),
    // FlowComment.TRANSFER.getType(),
    // commentBuilder.toString());
    // // Set to current
    // taskService.setOwner(bo.getTaskId(), TaskUtils.getUserId());
    // // task
    // taskService.setAssignee(bo.getTaskId(), bo.getUserId());
    // // Set tasknode
    // bo.setTaskName(task.getName());
    // // Process user
    // if (!copyService.makeCopy(bo)) {
    // throw new RuntimeException(" taskfailed");
    // }
    // }

    /**
     *
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
                // StringUtils.isBlank(flowTaskVo.getComment()) ? " " :
                // flowTaskVo.getComment());
                runtimeService.setVariable(processInstance.getId(), ProcessConstants.PROCESS_STATUS_KEY,
                        ProcessStatus.CANCELED.getStatus());
                for (Task task : taskList) {
                    taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
                            FlowComment.STOP.getType(), "取消流程");
                }
                // 1. new work order
                wfWorkOrderService.updateWorkOrderToPending(
                        taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                                .processInstanceId(bo.getProcInsId()).singleResult(),
                        WorkOrderStatus.CLOSED.getStatus(), null);
                // Get current workflow after node
                String endId = endNodes.get(0).getId();
                List<Execution> executions = runtimeService.createExecutionQuery()
                        .parentId(processInstance.getProcessInstanceId()).list();
                List<String> executionIds = new ArrayList<>();
                executions.forEach(execution -> executionIds.add(execution.getId()));
                // workflow to already finish
                runtimeService.createChangeActivityStateBuilder()
                        .moveExecutionsToSingleActivityId(executionIds, endId).changeState();
            }
        }
    }

    /**
     * workflow
     *
     * @param taskBo parameter
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeProcess(WfTaskBo taskBo) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        String procInsId = taskBo.getProcInsId();
        String taskId = taskBo.getTaskId();
        // Validate workflowwhether finish
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceId(procInsId)
                .active()
                .singleResult();
        if (ObjectUtil.isNull(processInstance)) {
            throw new RuntimeException("流程已结束或已挂起，无法执行撤回操作");
        }
        // Get taskinstance
        HistoricTaskInstance currTaskIns = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .taskAssignee(TaskUtils.getUserId())
                .singleResult();
        if (ObjectUtil.isNull(currTaskIns)) {
            throw new RuntimeException("当前任务不存在，无法执行撤回操作");
        }
        // Get bpmn model
        BpmnModel bpmnModel = repositoryService.getBpmnModel(currTaskIns.getProcessDefinitionId());
        UserTask currUserTask = ModelUtils.getUserTaskByKey(bpmnModel, currTaskIns.getTaskDefinitionKey());
        // find usertask
        List<UserTask> nextUserTaskList = ModelUtils.findNextUserTasks(currUserTask);
        List<String> nextUserTaskKeys = nextUserTaskList.stream().map(UserTask::getId).collect(Collectors.toList());

        // Get current node after already workflowhistory node
        List<HistoricTaskInstance> finishedTaskInsList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(procInsId)
                .taskCreatedAfter(currTaskIns.getEndTime())
                .finished()
                .list();
        for (HistoricTaskInstance finishedTaskInstance : finishedTaskInsList) {
            // already workflowhistory nodewhether in in
            if (CollUtil.contains(nextUserTaskKeys, finishedTaskInstance.getTaskDefinitionKey())) {
                throw new RuntimeException("下一流程已处理，无法执行撤回操作");
            }
        }
        // Get all tasknode, need to task
        List<Task> activateTaskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(procInsId).list();
        List<String> revokeExecutionIds = new ArrayList<>();
        identityService.setAuthenticatedUserId(TaskUtils.getUserId());
        for (Task task : activateTaskList) {
            // tasknodewhether in in , if in , need to node
            if (CollUtil.contains(nextUserTaskKeys, task.getTaskDefinitionKey())) {
                // approvalinfo
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
     * Get workflow
     *
     * @param processId
     * @return
     */
    @Override
    public InputStream diagram(String processId) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        String processDefinitionId;
        // Get current workflow instance
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(processId).singleResult();
        // if workflow already finish, finishnode
        if (Objects.isNull(processInstance)) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId())
                    .processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        } else {// if workflow finish, current node
            // workflow instance ID current ActivityId
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }

        // node
        List<HistoricActivityInstance> highLightedFlowList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();

        List<String> highLightedFlows = new ArrayList<>();
        List<String> highLightedNodes = new ArrayList<>();
        //
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            if ("sequenceFlow".equals(tempActivity.getActivityType())) {
                //
                highLightedFlows.add(tempActivity.getActivityId());
            } else {
                // node
                highLightedNodes.add(tempActivity.getActivityId());
            }
        }

        // Get workflow
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
        // Get Custom Generate
        ProcessDiagramGenerator diagramGenerator = new CustomProcessDiagramGenerator();
        return diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodes, highLightedFlows,
                configuration.getActivityFontName(),
                configuration.getLabelFontName(), configuration.getAnnotationFontName(), configuration.getClassLoader(),
                1.0, true);

    }

    /**
     * Get workflow variable
     *
     * @param taskId taskID
     * @return workflow variable
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
    // * task
    // *
    // * @param processInstance workflow instance
    // * @param variables workflowparameter
    // */
    // @Override
    // public void startFirstTask(ProcessInstance processInstance, Map<String,
    // Object> variables) {
    // // usertask to , task
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
    // .getType(), LoginHelper.getNickName() + " workflow ");
    // taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
    // FlowComment.NORMAL
    // .getType(), LoginHelper.getNickName() + " workflow ");
    // taskService.complete(task.getId(), variables);
    // }
    // }
    // }
    // }

    /**
     * task
     *
     * @param processInstance workflow instance
     * @param variables workflowparameter
     */
    @Override
    public void startFirstTask(ProcessInstance processInstance, Map<String, Object> variables, SysUser sysUser) {
        // usertask to , task
        List<Task> tasks = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInstance.getProcessInstanceId()).list();
        if (CollUtil.isNotEmpty(tasks)) {
            String userIdStr = (String) variables.get(TaskConstants.PROCESS_INITIATOR);
            identityService.setAuthenticatedUserId(sysUser.getUserId());
            for (Task task : tasks) {
                BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
                log.info("==========model:{}", ModelUtils.getBpmnXmlStr(model));
                // Check current node approverwhether is
                boolean a = sysUser.getUserId().equals(task.getAssignee());
                if (StrUtil.equals(task.getAssignee(), userIdStr)) {
                    if (sysUser != null) {
                        taskService.addComment(task.getId(), processInstance.getProcessInstanceId(),
                                FlowComment.NORMAL.getType(), sysUser.getUserName() + "发起流程申请");
                    }
                    taskService.complete(task.getId(), variables);
                    // after Query
                    List<Task> nextTasks = taskService.createTaskQuery()
                            .processInstanceId(processInstance.getId())
                            .active()
                            .list();
                    System.out.println("complete 后 active 任务数量：" + nextTasks.size());
                    nextTasks.forEach(t -> System.out
                            .println("taskId=" + t.getId() + ", taskDefKey=" + t.getTaskDefinitionKey()));

                    // Get new node
                    List<Task> newTaskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                            .processInstanceId(processInstance.getId()).active().list();
                    // current node approver is , page approver new node
                    newTaskList.stream().forEach(newTask -> {
                        if (a) {
                            String nextUserIds = (String) variables.get("nextUserIds");
                            if (StringUtils.isNotBlank(nextUserIds)) {
                                String[] userIdArray = nextUserIds.split(",");
                                List<String> candidateUserIds = Arrays.asList(userIdArray);
                                for (String userId : candidateUserIds) {
                                    // null / empty approver, workflow
                                    taskService.setAssignee(newTask.getId(), null);
                                    taskService.addCandidateUser(newTask.getId(), userId);
                                }
                            }
                            // need to , Set assignee
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
                        // Push method
//                        buildAndSendUnifiedMessage(newTask, newTask.getAssignee(), notifyAllSteps, sysUser);
                    });
                }
            }
        }
    }

    /**
     * taskapprover Set approver
     *
     * @param bpmnModel BPMNmodel
     * @param processInsId workflow instance ID
     * @param userIds user ID ( )
     * @param candidateUsers user ID ( is empty, Set user)
     * @param isPushMessage whether notification
     * @param sysUser current user ( )
     */
    private void assignORCandidateUserNextUsers(BpmnModel bpmnModel, String processInsId, String userIds,
            List<String> candidateUsers, List<String> candidateGroups,
            boolean isPushMessage, SysUser sysUser) {
        // 1. Query current task list
        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInsId)
                .list();
        if (CollUtil.isEmpty(tasks)) {
            return;
        }

        // 2. Process user ( )
        if (CollUtil.isNotEmpty(candidateUsers)) {
            handleCandidateUsers(bpmnModel, tasks, candidateUsers);
        } else if (CollUtil.isNotEmpty(candidateGroups)) {
            CandidateNextGroups(bpmnModel, processInsId, candidateGroups, sysUser);
        } else {
            handleDirectAssignment(bpmnModel, tasks, userIds);
        }

        // if (CollUtil.isEmpty(candidateGroups)) {
        // // 3. notification
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
     * Process userSet
     */
    private void handleCandidateUsers(BpmnModel bpmnModel, List<Task> tasks, List<String> candidateUsers) {
        Iterator<Task> iterator = tasks.iterator();
        // 1. new work order to
        wfWorkOrderService.updateWorkOrderToPending(tasks.get(0), WorkOrderStatus.PENDING_ORDERS.getStatus(), null);

        while (iterator.hasNext()) {
            Task task = iterator.next();
            String taskDefKey = task.getTaskDefinitionKey();

            // non- instancetask: user
            if (!ModelUtils.isMultiInstance(bpmnModel, taskDefKey)) {
                taskService.setAssignee(task.getId(), null);
                candidateUsers.forEach(userId -> taskService.addCandidateUser(task.getId(), userId));
                iterator.remove();
            }
        }

        // instancetask: userinstance
        if (CollUtil.isNotEmpty(tasks)) {
            tasks.forEach(task -> {
                Map<String, Object> variables = new HashMap<>();
                variables.put(ProcessConstants.USER_TYPE_USERS, candidateUsers); // usercollection
                runtimeService.addMultiInstanceExecution(
                        task.getTaskDefinitionKey(),
                        task.getProcessInstanceId(),
                        variables);
            });
        }
    }

    /**
     * Process ( )
     */
    private void handleDirectAssignment(BpmnModel bpmnModel, List<Task> tasks, String userIds) {
        Queue<String> assignIds = CollUtil.newLinkedList(userIds.split(","));
        if (tasks.size() == assignIds.size()) {
            tasks.forEach(task -> taskService.setAssignee(task.getId(), assignIds.poll()));
            return;
        }
        // 1. new work order to Process in
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
     * Set task approval (department)
     *
     * @param bpmnModel BPMNmodel
     * @param processInsId workflow instance ID
     * @param candidateGroups department ID ( )
     * @param sysUser current user ( )
     */
    private void CandidateNextGroups(BpmnModel bpmnModel, String processInsId, List<String> candidateGroups, SysUser sysUser) {
        // 1. Query current workflow instance all task
        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInsId)
                .list();
        // 1. new work order to
        wfWorkOrderService.updateWorkOrderToPending(tasks.get(0), WorkOrderStatus.PENDING_ORDERS.getStatus(), null);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        if (CollUtil.isEmpty(candidateGroups)) {
            throw new RuntimeException("候选部门ID不能为空");
        }
        // 3. task, Set candidate group ( Process non- instancetask)
        Iterator<Task> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            String taskDefKey = task.getTaskDefinitionKey();

            // 3.1 non- instancetask: Set candidate group
            if (!ModelUtils.isMultiInstance(bpmnModel, taskDefKey)) {
                taskService.setAssignee(task.getId(), null);
                for (String groupId : candidateGroups) {
                    taskService.addCandidateGroup(task.getId(), groupId);
                }
                iterator.remove();
            }
        }

        // 4. Process instancetask ( candidate group)
        if (CollUtil.isNotEmpty(tasks)) {
            tasks.forEach(task -> {
                Map<String, Object> variables = new HashMap<>();
                variables.put(ProcessConstants.USER_TYPE_ROUPS, candidateGroups); // collection
                runtimeService.addMultiInstanceExecution(
                        task.getTaskDefinitionKey(), // all task
                        task.getProcessInstanceId(),
                        variables);
            });
        }
    }

    /**
     * workflowpdf1
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
     * Get workflow new node approverinfo
     *
     * @param processInstanceId
     * @return
     */
    @Override
    public List<String> getApproverIds(String processInstanceId) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        List<String> approvers = new ArrayList<>();
        // 1. Get workflow instance
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            // 2. Get current task
            Task currentTask = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (currentTask != null) {
                // 3. Get current task approverinfo
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                for (IdentityLink identityLink : identityLinks) {
                    if (IdentityLinkType.CANDIDATE.equals(identityLink.getType())) {
                        if (StringUtils.isNotBlank(identityLink.getUserId())) {
                            approvers.add(identityLink.getUserId()); // Get approver user ID
                        } else if (StringUtils.isNotBlank(identityLink.getGroupId())
                                && identityLink.getGroupId().startsWith("ROLE")) {
                            int startIndex = "ROLE".length();
                            Long roleId = Long.valueOf(identityLink.getGroupId().substring(startIndex));
                            // Get role all
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
     * tokenGet userinfo
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
     * Get tasknode noNotifyAllSteps configuration
     * from tasknode extension properties in configuration, Check whether need to notification
     *
     * @param task taskobject
     * @return if configuration notification true, false
     */
    private Boolean getNoNotifyAllSteps(Task task) {
        // Get workflow definitionmodel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        // Get current tasknode
        FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());

        if (flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;

            // from extension properties in noNotifyAllSteps
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

        // false ( need to notification)
        return false;
    }

    /**
     * Get workflow
     *
     * @param historicProcessInstance workflow instance
     * @param sysUser current user
     * @return workflow
     */
    public String getProcessName(HistoricProcessInstance historicProcessInstance, SysUser sysUser) {
        return repositoryService.createDeploymentQuery()
                .deploymentId(historicProcessInstance.getDeploymentId())
                .deploymentTenantId(sysUser.getTenantId())
                .singleResult()
                .getName();
    }

    /**
     * workflow definition IDGet workflow definition in variable
     *
     * @param processDefinitionId workflow definition ID
     */
    public Map<String, String> getProcessDefinitionProperties(String processDefinitionId) {
        // 1. Get workflow definition elementinfo
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        // 2. BPMNModel Get extension properties
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        // Get workflow definition (Process)object
        org.flowable.bpmn.model.Process process = bpmnModel.getProcessById(processDefinition.getKey());

        Map<String, String> properties = new HashMap<>();
        if (process != null) {
            // Get all flowable:property element
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
     * Build
     *
     * @param task current task
     * @param receive_userId id
     * @param notifyAllSteps whether Push
     */
    public void buildAndSendUnifiedMessage(Task task, String receive_userId, boolean notifyAllSteps, SysUser sysUser) {
        // from tasknode extension properties in Get noNotifyAllSteps configuration
        Boolean noNotifyAllSteps = getNoNotifyAllSteps(task);
        if (noNotifyAllSteps) {
            log.warn("任务节点配置了 noNotifyAllSteps，无需推送信息");
            return;
        }

        if (StringUtils.isBlank(receive_userId)) {
            log.warn("下一节点无审批人，无推送目标");
            return;
        }

        // Query workflow instance variable
        HistoricProcessInstance historicProcessInstance =
            wfInstanceService.getHistoricProcessInstanceById(task.getProcessInstanceId(), sysUser);
        Map<String, Object> processVariables = runtimeService.getVariables(historicProcessInstance.getId());
        String applicationId = Optional.ofNullable(processVariables.get("appid")).map(Objects::toString).orElse(null);
        if (StringUtils.isBlank(applicationId)) {
            log.error("没找到应用ID");
            return;
        }
        // Get workflow variable
        String receiveUserName = sysUserService.selectUserNameById(receive_userId);

        // Get workflow
        String processName = getProcessName(historicProcessInstance, sysUser);

        // Build content value
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

            // data.list array
            JsonNode listNode = root.path("data").path("list");
            if (!listNode.isArray()) {
                log.error("返回结构里找不到 data.list"+root.toString());
                return;
            }
            // list array
            for (JsonNode item : listNode) {
                // sub node channel_list
                JsonNode channelList = item.path("channel_list");

                // only channel_list in is array, to element ,
                if (channelList.isArray() && channelList.size() > 0) {
                    for (JsonNode jsonNode : channelList) {
                        if (jsonNode.path("channel_types").asText().equals("1")) {
                            // current item duct_code
                            ductCode = item.path("duct_code").asText();
                            // channel_list element channel_code
                            channelCode = jsonNode.path("channel_code").asText();
                            break; // then exit loop
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("请求或解析返回数据失败", e);
        }
        // Build Push parameter
        Map<String, Object> params = ApiHeaderUtil.buildParams(ductCode, // channel
                channelCode, //
                receive_userId, // user ID
                receiveUserName, // username
                processName, //
                content, //
                "1", //
                task.getProcessInstanceId(), // workflow instance ID
                task.getId(), // taskID
                templateCode, variableValue);

        // Push method
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
