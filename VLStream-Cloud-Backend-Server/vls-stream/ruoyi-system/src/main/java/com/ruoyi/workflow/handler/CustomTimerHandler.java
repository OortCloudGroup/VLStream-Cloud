/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class CustomTimerHandler  extends TimerEventHandler implements JobHandler {


    public static final String TYPE = "customJobType";

    // Custom
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity jobEntity, String configuration, VariableScope variableScope, CommandContext commandContext) {
        try {
            log.info("✅ 任务执行成功: {}", configuration);
            // ( 5 after)
            int waitTimeInSeconds = 20;
            Date nextDueDate = new Date(System.currentTimeMillis() + waitTimeInSeconds * 1000);
            ProcessEngineConfigurationImpl config = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            ManagementService managementService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getManagementService();
//            ObjectMapper objectMapper = new ObjectMapper();
//            SysUser sysUser = objectMapper.readValue(configuration, SysUser.class);
            log.info("🛠️ 开始创建定时任务，处理器类型: {}", CustomTimerHandler.TYPE);
            managementService.executeCommand(context -> {
                // Get taskservice
                TimerJobService timerJobService = config.getJobServiceConfiguration().getTimerJobService();
                // task
                TimerJobEntity timerJob = timerJobService.createTimerJob();
                timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
//          timerJob.setExclusive(true);
                timerJob.setDuedate(new Date());
                timerJob.setJobHandlerType(CustomTimerHandler.TYPE);
                timerJob.setJobHandlerConfiguration("");
                timerJob.setTenantId(jobEntity.getTenantId());
                timerJob.setProcessDefinitionId(jobEntity.getProcessDefinitionId());
//            timerJob.setRepeat(cycle);
                timerJobService.scheduleTimerJob(timerJob);
                Job job = managementService.createDeadLetterJobQuery().jobId(timerJob.getId()).singleResult();
                if(ObjectUtil.isNotNull(job)){
                    managementService.moveDeadLetterJobToExecutableJob(timerJob.getId(), 3);
                }else {
                    System.out.println("任务不在死信表中，可能已执行或不存在。2");
                }
                return null;
            });

            log.info("🔄 任务重新调度，下一次执行时间: {}", nextDueDate);
        } catch (Exception e) {
            log.error("❌ 任务执行失败: {}", e.getMessage(), e);
        }
    }
}
