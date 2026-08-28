/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
        // Get workflow definition ID
        String processDefinitionId = delegateTask.getProcessDefinitionId();

        // RepositoryService Get BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        // Get workflow definition main workflow object
        Process process = bpmnModel.getMainProcess();
        // Get Customproperty
        String notifyAllSteps = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        System.out.println("notifyAllSteps 属性值: " + notifyAllSteps);  // property valuewhether correct

        runtimeService.setVariable(delegateTask.getExecutionId(),"notifyAllSteps",notifyAllSteps);
        String  notifyAllSteps1 = (String) runtimeService.getVariable(delegateTask.getExecutionId(), "notifyAllSteps");
        System.out.println("notifyAllSteps1 属性值: " + notifyAllSteps1);
        // Get current tasknodeinfo
        String currentTaskId = delegateTask.getId();
        System.out.println("当前任务ID: " + currentTaskId);
    }
}

