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
 * 超时自动通过监听器
 * 处理审批节点超时后的自动审批通过
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

            // 1. 获取TaskService和IdentityService
            TaskService taskService = applicationContext.getBean(TaskService.class);
            IdentityService identityService = applicationContext.getBean(IdentityService.class);

            // 2. 查询当前流程实例的活动任务
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(execution.getProcessInstanceId())
                    .list();

            if (tasks.isEmpty()) {
                log.info("超时自动通过监听器：任务已不存在（可能已被人工处理或其他超时处理器处理），跳过执行");
                return;
            }

            // 3. 对每个任务执行自动通过
            for (Task task : tasks) {
                autoCompleteTask(task, taskService, identityService, execution);
            }

            log.info("超时自动通过监听器执行完成，共处理 {} 个任务", tasks.size());

        } catch (Exception e) {
            log.error("超时自动通过监听器执行失败", e);
            // 不抛出异常，避免影响流程执行
        }
    }

    /**
     * 自动完成任务
     */
    private void autoCompleteTask(Task task, TaskService taskService, IdentityService identityService,
            DelegateExecution execution) {
        try {
            log.info("开始自动通过任务：taskId={}, taskName={}", task.getId(), task.getName());

            // 1. 设置系统用户为操作人（使用系统标识）
            String systemUserId = "system";
            String tenantId = task.getTenantId();

            // 尝试从租户中获取系统用户（如果有配置）
            if (tenantId != null && !tenantId.isEmpty()) {
                systemUserId = "system_" + tenantId;
            }

            identityService.setAuthenticatedUserId(systemUserId);

            // 2. 添加审批意见
            String comment = "审批超时系统自动通过";
            taskService.addComment(task.getId(), task.getProcessInstanceId(),
                    FlowComment.NORMAL.getType(), comment);

            // 3. 设置任务的 assignee（如果当前没有）
            if (task.getAssignee() == null || task.getAssignee().trim().isEmpty()) {
                taskService.setAssignee(task.getId(), systemUserId);
            }

            // 4. 设置流程变量，标记该任务为自动通过
            execution.setVariable("autoApproved_" + task.getId(), true);
            execution.setVariable("autoApproveReason", comment);

            // 5. 完成任务
            taskService.complete(task.getId());

            log.info("任务自动通过成功：taskId={}, taskName={}", task.getId(), task.getName());

        } catch (Exception e) {
            log.error("自动完成任务失败：taskId={}, taskName={}", task.getId(), task.getName(), e);
            throw new RuntimeException("自动完成任务失败: " + e.getMessage(), e);
        }
    }
}
