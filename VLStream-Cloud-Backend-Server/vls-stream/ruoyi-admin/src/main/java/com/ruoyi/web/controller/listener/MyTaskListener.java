/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.listener;


import com.ruoyi.flowable.factory.FlowServiceFactory;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
public class MyTaskListener extends FlowServiceFactory implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        // 获取流程定义ID
        String processDefinitionId = delegateTask.getProcessDefinitionId();

        // 使用 RepositoryService 获取 BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        // 获取流程定义的主流程对象
        Process process = bpmnModel.getMainProcess();
        // 获取自定义属性
        String notifyAllSteps = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        System.out.println("notifyAllSteps 属性值: " + notifyAllSteps);  // 确认属性值是否正确

        runtimeService.setVariable(delegateTask.getExecutionId(),"notifyAllSteps",notifyAllSteps);
        String  notifyAllSteps1 = (String) runtimeService.getVariable(delegateTask.getExecutionId(), "notifyAllSteps");
        System.out.println("notifyAllSteps1 属性值: " + notifyAllSteps1);
        // 获取当前任务节点信息
        String currentTaskId = delegateTask.getId();
        System.out.println("当前任务ID: " + currentTaskId);
    }
}

