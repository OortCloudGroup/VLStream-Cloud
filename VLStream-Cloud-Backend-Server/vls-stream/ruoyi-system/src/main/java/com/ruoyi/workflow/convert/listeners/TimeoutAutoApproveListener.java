/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.flowable.common.enums.FlowComment;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.api.Task;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * listener
 * Process approvalnode after approval
 */
@Slf4j
@Component
public class TimeoutAutoApproveListener implements JavaDelegate, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        TimeoutAutoApproveListener.applicationContext = applicationContext;
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            log.info("超时自动通过监听器开始执行，流程实例ID: {}", execution.getProcessInstanceId());

            // 1. Get TaskService and IdentityService
            TaskService taskService = applicationContext.getBean(TaskService.class);
            IdentityService identityService = applicationContext.getBean(IdentityService.class);

            // 2. Query current workflow instance task
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(execution.getProcessInstanceId())
                    .list();

            if (tasks.isEmpty()) {
                log.info("超时自动通过监听器：任务已不存在（可能已被人工处理或其他超时处理器处理），跳过执行");
                return;
            }

            // 3. each taskExecute
            for (Task task : tasks) {
                autoCompleteTask(task, taskService, identityService, execution);
            }

            log.info("超时自动通过监听器执行完成，共处理 {} 个任务", tasks.size());

        } catch (Exception e) {
            log.error("超时自动通过监听器执行失败", e);
            // , workflowExecute
        }
    }

    /**
     * task
     */
    private void autoCompleteTask(Task task, TaskService taskService, IdentityService identityService,
            DelegateExecution execution) {
        try {
            log.info("开始自动通过任务：taskId={}, taskName={}", task.getId(), task.getName());

            // 1. Set user to operation ( )
            String systemUserId = "system";
            String tenantId = task.getTenantId();

            // from in Get user (if configuration)
            if (tenantId != null && !tenantId.isEmpty()) {
                systemUserId = "system_" + tenantId;
            }

            identityService.setAuthenticatedUserId(systemUserId);

            // 2. approval
            String comment = "审批超时系统自动通过";
            taskService.addComment(task.getId(), task.getProcessInstanceId(),
                    FlowComment.NORMAL.getType(), comment);

            // 3. Set task assignee (if current )
            if (task.getAssignee() == null || task.getAssignee().trim().isEmpty()) {
                taskService.setAssignee(task.getId(), systemUserId);
            }

            // 4. Set workflow variable, task to
            execution.setVariable("autoApproved_" + task.getId(), true);
            execution.setVariable("autoApproveReason", comment);

            // 5. task
            taskService.complete(task.getId());

            log.info("任务自动通过成功：taskId={}, taskName={}", task.getId(), task.getName());

        } catch (Exception e) {
            log.error("自动完成任务失败：taskId={}, taskName={}", task.getId(), task.getName(), e);
            throw new RuntimeException("自动完成任务失败: " + e.getMessage(), e);
        }
    }
}
