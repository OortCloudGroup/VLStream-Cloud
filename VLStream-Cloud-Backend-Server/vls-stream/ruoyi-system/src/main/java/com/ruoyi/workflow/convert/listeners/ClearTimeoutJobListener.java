/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.common.utils.spring.SpringUtils;
import org.flowable.engine.ManagementService;
import org.flowable.job.api.Job;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClearTimeoutJobListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        ManagementService managementService =  SpringUtils.getBean(ManagementService.class);
        String procInsId = delegateTask.getProcessInstanceId();
        // 查所有挂在同一 boundaryEvent 的定时器
        List<Job> timer = managementService.createTimerJobQuery()
                                           .processInstanceId(procInsId)
                                           .handlerType("timer")
                                           .list();
        // 删除定时器
        for (Job tj : timer) {
            managementService.deleteTimerJob(tj.getId());
        }
    }
}
