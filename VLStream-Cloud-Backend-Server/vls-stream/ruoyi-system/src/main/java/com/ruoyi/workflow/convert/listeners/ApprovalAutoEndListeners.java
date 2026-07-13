/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.common.enums.FlowComment;
import com.ruoyi.flowable.common.enums.ProcessStatus;
import com.ruoyi.flowable.utils.ModelUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动拒绝监听器
 */
@Component
public class ApprovalAutoEndListeners  implements TaskListener{

    @Override
    public void notify(DelegateTask delegateTask) {
        ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        TaskService taskService = processEngineConfiguration.getTaskService();
        RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();

        String processInstanceId = delegateTask.getProcessInstanceId();
        // 添加审批意见
        taskService.addComment(delegateTask.getId(), processInstanceId,
            FlowComment.REJECT.getType(),"系统自动拒绝");
        // 设置流程状态为已终结
        runtimeService.setVariable(processInstanceId, ProcessConstants.PROCESS_STATUS_KEY,
            ProcessStatus.TERMINATED.getStatus());
        // 将拒绝节点的处理人信息存储到流程变量中
        runtimeService.setVariable(processInstanceId, "rejectAssignee", "系统自动拒绝");
        runtimeService.setVariable(processInstanceId, "rejectTaskId", delegateTask.getId());
        runtimeService.setVariable(processInstanceId, "rejectTaskName", delegateTask.getName());
        BpmnModel bpmnModel = repositoryService.getBpmnModel(delegateTask.getProcessDefinitionId());
        EndEvent endEvent = ModelUtils.getEndEvent(bpmnModel);
        List<String> executionIds = runtimeService
            .createExecutionQuery()
            .processInstanceId(delegateTask.getProcessInstanceId())
            .list()
            .stream()
            .map(Execution::getId)
            .collect(Collectors.toList());
        runtimeService.createChangeActivityStateBuilder().processInstanceId(delegateTask.getProcessInstanceId())
            .moveExecutionsToSingleActivityId(executionIds,endEvent.getId());
        // 替换原有移动执行实例的代码
        runtimeService.deleteProcessInstance(processInstanceId, "系统自动拒绝");
    }
}
