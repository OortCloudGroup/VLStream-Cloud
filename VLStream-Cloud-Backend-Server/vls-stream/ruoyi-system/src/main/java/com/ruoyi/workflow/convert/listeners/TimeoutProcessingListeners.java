/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.service.impl.WfTaskServiceImpl;
import lombok.Data;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * task Process listener
 */
/**
 * @deprecated already TimeoutNotificationListener and TimeoutAutoApproveListener
 * to , new item Process (timeoutHandlers)
 */
@Deprecated
@Data
@Component
public class TimeoutProcessingListeners implements JavaDelegate, ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private FixedValue approTimeoutProcessing;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TimeoutProcessingListeners.applicationContext = applicationContext;
    }

    @Override
    public void execute(DelegateExecution delegateExecution) {
        WfTaskServiceImpl wfTaskService = applicationContext.getBean(WfTaskServiceImpl.class);
        HistoryService historyService = applicationContext.getBean(HistoryService.class);
        TaskService taskService = applicationContext.getBean(TaskService.class);

        String approTimeoutProcessing2 = (String) approTimeoutProcessing.getValue(delegateExecution);

        String procInsId = delegateExecution.getProcessInstanceId();
        String taskId = taskService.createTaskQuery().processInstanceId(procInsId)
                .taskTenantId(delegateExecution.getTenantId()).list().stream().map(task -> task.getId())
                .collect(Collectors.joining(","));
        HistoricTaskInstance initiatorTask = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(procInsId).orderByHistoricTaskInstanceStartTime().asc().listPage(0, 1).stream()
                .findFirst().orElse(null);
        String initiatorTaskDefKey = null;
        if (initiatorTask != null) {
            initiatorTaskDefKey = initiatorTask.getTaskDefinitionKey(); // usertasknode ID
        }
        WfTaskBo bo = new WfTaskBo();
        bo.setProcInsId(procInsId);
        bo.setTaskId(taskId);
        bo.setTargetKey(initiatorTaskDefKey);
        bo.setVariables(delegateExecution.getVariables());
        SysUser sysUser = new SysUser();
        sysUser.setTenantId(initiatorTask.getTenantId());
        if (approTimeoutProcessing2.equals("2")) {//
            bo.setComment("任务超时退回");
            wfTaskService.taskReturn(bo, sysUser);
        } else {
            bo.setComment("任务超时驳回");
            wfTaskService.taskReject(bo, sysUser);
        }
    }
}
