/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.delegate;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.system.service.impl.SysDeptServiceImpl;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import io.micrometer.core.instrument.util.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.applet.AppletContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DynamicUserTaskCreationDelegate implements JavaDelegate, ApplicationContextAware {
    private static ApplicationContext applicationContext;
    public DynamicUserTaskCreationDelegate() {
        // No-args constructor
    }

    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        this.applicationContext = arg0;
    }

    @Override
    public void execute(DelegateExecution execution) {
        SysUserServiceImpl sysUserServiceImpl = (SysUserServiceImpl)applicationContext.getBean("sysUserServiceImpl");
        TaskService taskService = (TaskService)applicationContext.getBean(TaskService.class);
        RuntimeService runtimeService = (RuntimeService)applicationContext.getBean(RuntimeService.class);

        //
        String initiator = (String) execution.getVariable("initiator");
        List<SysUser> leaders = sysUserServiceImpl.getLeaders(initiator);

// // department hierarchy workflow variable
//        execution.setVariable("leaders", leaders);
        // Initialize complete variable to 1
        execution.setVariable("complete", "1");

        // Set approver ( departmentleader)
        if (!leaders.isEmpty()) {
            execution.setVariable("currentAssignee", initiator);
        }
//        if (!leaders.isEmpty()) {
//            execution.setVariable("currentAssignee", leaders.get(0));
//        }
    }
}
