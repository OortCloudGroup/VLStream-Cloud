/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.listener;

import com.esotericsoftware.minlog.Log;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.common.enums.FlowComment;
import com.ruoyi.flowable.core.FormConf;
import com.ruoyi.system.mapper.SysUserRoleViewMapper;
import com.ruoyi.system.service.impl.SysRoleServiceImpl;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import com.ruoyi.workflow.domain.vo.TaskDataPushVo;
import com.ruoyi.workflow.service.impl.WfProcessServiceImpl;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 每个操作结束后，推送操作数据和表单数据
 */
@Configuration
public class TaskDataPushListener implements TaskListener, ExecutionListener, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TaskDataPushListener.applicationContext = applicationContext;
    }

    private FixedValue callbackAddress;

    public FixedValue getCallbackAddress() {
        return callbackAddress;
    }

    public void setCallbackAddress(FixedValue callbackAddress) {
        this.callbackAddress = callbackAddress;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        Log.info("=================触发回调监听器=================");
        TaskDataPushVo taskDataPushVo = new TaskDataPushVo();
        Environment environment = (Environment) applicationContext.getBean("environment");
        TaskService taskService = applicationContext.getBean(TaskService.class);
        HistoryService historyService = applicationContext.getBean(HistoryService.class);
        RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
        SysUserServiceImpl sysUserService = (SysUserServiceImpl) applicationContext.getBean("sysUserServiceImpl");
        SysRoleServiceImpl iSysRoleService = (SysRoleServiceImpl) applicationContext.getBean("sysRoleServiceImpl");
        RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
        IdentityService identityService = applicationContext.getBean(IdentityService.class);
        RestTemplate restTemplate = (RestTemplate) applicationContext.getBean("restTemplate");
        WfProcessServiceImpl wfProcessService = (WfProcessServiceImpl) applicationContext.getBean("wfProcessServiceImpl");
        SysUserRoleViewMapper sysUserRoleViewMapper = applicationContext.getBean(SysUserRoleViewMapper.class);
        // 获取当前任务的信息
        String taskId = delegateTask.getId(); // 获取任务 ID
        String taskName = delegateTask.getName(); // 获取任务名称
        String assignee = delegateTask.getAssignee(); // 获取任务的办理人
        Set<IdentityLink> candidates = delegateTask.getCandidates();// 获取当前任务的候选人集合
        String processDefinitionId = delegateTask.getProcessDefinitionId();
        String taskDefKey = delegateTask.getTaskDefinitionKey();//获取任务id
        String eventName = delegateTask.getEventName();//获取当前监听器类型

        //如果办理人为空，获取候选人并设置为办理人
        // 如果存在候选组
        for (IdentityLink candidate : candidates) {
            if (candidate.getGroupId() != null) {
                String groupId = candidate.getGroupId();
                int startIndex = "ROLE".length();
                Long roleId = Long.valueOf(groupId.substring(startIndex));
                // 用户服务可以通过组ID查询该组的用户
                List<String > usersInRoleIds =  sysUserRoleViewMapper.selectUserIdsByRoleId(roleId);

                if (!usersInRoleIds.isEmpty()) {
                    assignee=usersInRoleIds.get(0);
                    Log.info("任务的候选办理人用户ID: " + usersInRoleIds.get(0));
                    break; // 退出循环，只设置一个办理人
                }
            }
        }


// 判断是否是指定用户审批
        // 获取流程模型 BpmnModel 对象
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        // 查找当前任务的 BPMN 元素定义
        FlowElement flowElement = bpmnModel.getFlowElement(delegateTask.getTaskDefinitionKey());
        if (flowElement instanceof UserTask) {
            UserTask userTask = (org.flowable.bpmn.model.UserTask) flowElement;
            // 获取任务的扩展属性，例如 assigneeUsers
            String userTaskAssignee = userTask.getAssignee();
            if (StringUtils.isNotBlank(userTaskAssignee) && assignee.equals(userTaskAssignee)) {
                taskDataPushVo.setAssigneeType("2");
            } else {
                taskDataPushVo.setAssigneeType("1");
            }
        }
//        String[] split = assignee.split(",");
//        List<Long> longs = iSysRoleService.selectRoleListByUserId(split[0]);
        SysUser sysUser = sysUserService.selectUserById(assignee);
        // 获取身份证号
        String idcard = sysUser.getIdcard();
        String processInstanceId = delegateTask.getProcessInstanceId(); // 获取流程实例 ID
        List<Comment> commentList = taskService.getProcessInstanceComments(processInstanceId);
        for (Comment comment : commentList) {
            if (StringUtils.isNotBlank(comment.getTaskId()) && comment.getTaskId().equals(delegateTask.getId())) {
                taskDataPushVo.setFullMessage(comment.getFullMessage());
                taskDataPushVo.setOperateType(FlowComment.getRemarkByType(comment.getType()));
            }
        }
        // 获取历史流程实例对象
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceTenantId(delegateTask.getTenantId())
            .processInstanceId(delegateTask.getProcessInstanceId())
            .singleResult();

        List<Object> formConfs = wfProcessService.processFormList(bpmnModel, historicProcessInstance);
        List<Map<String, Object>> formDataMap = new ArrayList<>();

        for (Object formConf : formConfs) {
            FormConf formConfObj = (FormConf) formConf;  // 将 Object 类型转换为 FormConf
            List<Map<String, Object>> fields = formConfObj.getFields();
            Map<String, Object> fieldMap = new HashMap<>();
            for (Map<String, Object> field : fields) {
                // 假设每个 field 中包含 key 和 value 属性
                String key = (String) field.get("__vModel__");
                Object defaultValue = runtimeService.getVariable(processInstanceId, key);
                fieldMap.put(key, defaultValue);
            }
            formDataMap.add(fieldMap);
        }

        // 整合数据
        // taskDataPushVo.setNextUserIds();
        taskDataPushVo.setTaskId(taskId);
        taskDataPushVo.setTaskName(taskName);
        taskDataPushVo.setAssignee(assignee);
        if ("管理员审核".equals(taskName)) {
            taskDataPushVo.setAssigneeType("2");
        }

//        taskDataPushVo.setAssigneeType(longs.contains("1") ? "2" : "1");
        taskDataPushVo.setAssigneeIdCard(idcard);
        taskDataPushVo.setProcessInstanceId(processInstanceId);
        taskDataPushVo.setFormDataMap(formDataMap);

        Log.info("taskDataPushVo= " + taskDataPushVo);
        String pushUrl = getUrl(processDefinitionId, eventName);
        if (!StringUtils.isNotBlank(pushUrl)) {
            throw new RuntimeException("未获取到回调地址，请在流程绘制页面添加");
        }
        try {
            Log.info("回调地址为: " + pushUrl);
            if (StringUtils.isNotBlank(pushUrl)) {
                restTemplate.postForObject(pushUrl, taskDataPushVo, Void.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("回调失败:" + e.getMessage());

        }
    }

    @Override
    public void notify(DelegateExecution delegateExecution) {
        TaskService taskService = applicationContext.getBean(TaskService.class);
        SysUserServiceImpl sysUserService = (SysUserServiceImpl) applicationContext.getBean("sysUserServiceImpl");
        SysRoleServiceImpl iSysRoleService = (SysRoleServiceImpl) applicationContext.getBean("sysRoleServiceImpl");
        RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
        HistoryService historyService = applicationContext.getBean(HistoryService.class);
        RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
        WfProcessServiceImpl wfProcessService = (WfProcessServiceImpl) applicationContext.getBean("wfProcessServiceImpl");
        RestTemplate restTemplate = (RestTemplate) applicationContext.getBean("restTemplate");
        Log.info("=================触发了流程结束监听=================");
        TaskDataPushVo taskDataPushVo = new TaskDataPushVo();
        // 从DelegateExecution中获取流程实例相关信息
        String processInstanceId = delegateExecution.getProcessInstanceId();
        String processDefinitionId = delegateExecution.getProcessDefinitionId();
        String eventName = delegateExecution.getEventName();//获取当前监听器类型
        // 获取拒绝节点的处理人信息
//        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
//            .processInstanceId(processInstanceId)
//            .taskDeleteReason(FlowComment.REJECT.getType()) // 假设拒绝节点的任务类型是 REJECT
//            .list();
        // 获取拒绝节点的处理人信息从流程变量中
        String assignee = (String) runtimeService.getVariable(processInstanceId, "rejectAssignee");
        String rejectTaskId = (String) runtimeService.getVariable(processInstanceId, "rejectTaskId");
        taskDataPushVo.setTaskId(rejectTaskId);
        String rejectTaskName = (String) runtimeService.getVariable(processInstanceId, "rejectTaskName");
        taskDataPushVo.setTaskName(rejectTaskName);
        // 获取当前节点的ID
        String currentActivityId = delegateExecution.getCurrentActivityId();
        if (StringUtils.isNotBlank(assignee)) { // 手动结束的流程
            taskDataPushVo.setAssignee(assignee);
            // 获取身份证号
            SysUser sysUser = sysUserService.selectUserById(assignee);
            taskDataPushVo.setAssigneeIdCard(sysUser.getIdcard());
            // 判断是否是管理员
            String[] split = assignee.split(",");
            List<Long> longs = iSysRoleService.selectRoleListByUserId(split[0]);
            // 判断是否是指定用户审批
            // 获取流程模型 BpmnModel 对象
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            // 查找当前任务的 BPMN 元素定义
            Task task = taskService.createTaskQuery()
                .taskTenantId(delegateExecution.getTenantId())
                .taskId(rejectTaskId)
                .singleResult();
            FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
            if (flowElement instanceof UserTask) {
                UserTask userTask = (org.flowable.bpmn.model.UserTask) flowElement;
                // 获取任务的扩展属性，例如 assigneeUsers
                if (flowElement instanceof UserTask) {
                    // 获取任务的扩展属性，例如 assigneeUsers
                    String userTaskAssignee = userTask.getAssignee();
                    if (assignee.equals(userTaskAssignee)) {
                        taskDataPushVo.setAssigneeType("2");
                    } else {
                        taskDataPushVo.setAssigneeType("1");
                    }
                }
            }


//            taskDataPushVo.setAssigneeType(longs.contains("1") ? "2" : "1");
            List<Comment> commentList = taskService.getProcessInstanceComments(processInstanceId);
            for (Comment comment : commentList) {
                if (StringUtils.isNotBlank(comment.getTaskId()) && comment.getTaskId().equals(rejectTaskId)) {
                    taskDataPushVo.setFullMessage(comment.getFullMessage());
                }
            }
            taskDataPushVo.setProcessInstanceId(processInstanceId);
            taskDataPushVo.setOperateType("拒绝");

            // ... 其他数据填充逻辑，可能需要从历史记录或流程变量中获取
            // 获取流程模型 BpmnModel对象
            // 获取历史流程实例对象
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(delegateExecution.getTenantId())
                .processInstanceId(delegateExecution.getProcessInstanceId())
                .singleResult();
            List<Object> formConfs = wfProcessService.processFormList(bpmnModel, historicProcessInstance);
            List<Map<String, Object>> formDataMap = new ArrayList<>();

            for (Object formConf : formConfs) {
                FormConf formConfObj = (FormConf) formConf;  // 将 Object 类型转换为 FormConf
                List<Map<String, Object>> fields = formConfObj.getFields();

                Map<String, Object> fieldMap = new HashMap<>();
                for (Map<String, Object> field : fields) {
                    // 假设每个 field 中包含 key 和 value 属性
                    String key = (String) field.get("__vModel__");
                    Object defaultValue = runtimeService.getVariable(processInstanceId, key);
                    fieldMap.put(key, defaultValue);
                }
                formDataMap.add(fieldMap);
            }
            taskDataPushVo.setFormDataMap(formDataMap);
            Log.info("流程结束时的数据推送对象: " + taskDataPushVo);
            // 执行回调逻辑
            String pushUrl = getUrl(processDefinitionId, eventName);
            if (!StringUtils.isNotBlank(pushUrl)) {
                throw new RuntimeException("未获取到回调地址，请在流程绘制页面添加");
            }
            try {
                Log.info("流程结束回调地址为: " + pushUrl);
                if (StringUtils.isNotBlank(pushUrl)) {
                    restTemplate.postForObject(pushUrl, taskDataPushVo, Void.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("流程结束回调失败", e);
            }
        }
    }


    private String getUrl(String processDefinitionId, String eventName) {
        RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        if (bpmnModel != null) {
            List<Process> processes = bpmnModel.getProcesses();
            for (Process process : processes) {
                List<ExtensionElement> properties = process.getExtensionElements().get("properties");

                if (properties != null && !properties.isEmpty()) {
                    StringBuilder dataBuilder = new StringBuilder();

                    for (ExtensionElement property : properties) {
                        List<ExtensionElement> propertyElements = property.getChildElements().get("property");

                        if (propertyElements != null && !propertyElements.isEmpty()) {
                            for (ExtensionElement propertyElement : propertyElements) {
                                String propertyName = propertyElement.getAttributeValue(null, "name");
                                String propertyValue = propertyElement.getAttributeValue(null, "value");
                                if ("callbackAddress".equals(propertyName) && propertyValue != null) {
                                    dataBuilder.append(propertyValue).append("\n");
                                }
                                if ("areaNodeNotice".equals(propertyName) && propertyValue != null && StringUtils.isNotBlank(eventName) && "create".equals(eventName)) {
                                    dataBuilder.setLength(0);
                                    dataBuilder.append(propertyValue).append("\n");
                                }
                            }
                        }
                    }
                    if (dataBuilder.length() > 0) {
                        return dataBuilder.toString();
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

}
