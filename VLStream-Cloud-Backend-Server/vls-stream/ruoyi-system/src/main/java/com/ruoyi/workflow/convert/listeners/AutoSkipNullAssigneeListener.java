/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.common.utils.StringUtils;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
public class AutoSkipNullAssigneeListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        // 直接从 DelegateTask 上拿分配人
        String assignee = delegateTask.getAssignee();

        // 2. 没有候选用户和候选组
        boolean noCandidateUsers = delegateTask.getCandidates()
                                               .stream().noneMatch(identityLink -> "candidate".equals(identityLink.getType()) && identityLink.getUserId() != null);

        boolean noCandidateGroups = delegateTask.getCandidates()
                                                .stream().noneMatch(identityLink -> "candidate".equals(identityLink.getType()) && identityLink.getGroupId() != null);

        if (StringUtils.isBlank(assignee)&& noCandidateUsers && noCandidateGroups) {
            ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
            TaskService taskService = processEngineConfiguration.getTaskService();
            taskService.complete(delegateTask.getId());
            taskService.addComment(delegateTask.getId(), delegateTask.getProcessInstanceId(),
                "无审批人以及候选审批人，系统自动跳过");
        }
    }
}
