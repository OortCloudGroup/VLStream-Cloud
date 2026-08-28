/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
        // all in boundaryEvent
        List<Job> timer = managementService.createTimerJobQuery()
                                           .processInstanceId(procInsId)
                                           .handlerType("timer")
                                           .list();
        // Delete
        for (Job tj : timer) {
            managementService.deleteTimerJob(tj.getId());
        }
    }
}
