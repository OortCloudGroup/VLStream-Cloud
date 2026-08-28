/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

//package com.ruoyi.web.controller.delegate;
//
//import org.flowable.engine.TaskService;
//import org.flowable.engine.delegate.DelegateExecution;
//import org.flowable.engine.delegate.JavaDelegate;
//import org.flowable.task.api.Task;
//import org.springframework.beans.BeansException;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class LoopConditionDelegate implements JavaDelegate, ApplicationContextAware {
//    private static ApplicationContext applicationContext;
//    public LoopConditionDelegate() {
// // No-args constructor
//    }
//    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
//        this.applicationContext = arg0;
//    }
//    @Override
//    public void execute(DelegateExecution execution) {
// // assuming department hierarchy
//        List<String> departmentHierarchy = (List<String>) execution.getVariable("departmentHierarchy");
//        Integer currentLevel = (Integer) execution.getVariable("currentLevel");
//
// // whether layer
//        if (currentLevel < departmentHierarchy.size() - 1) {
//            currentLevel++;
//            execution.setVariable("currentAssignee", departmentHierarchy.get(currentLevel));
//            execution.setVariable("currentLevel", currentLevel);
//            execution.setVariable("loopCondition", true);
//
//            TaskService taskService = (TaskService)applicationContext.getBean(TaskService.class);
// // // Set current task Process
////            String currentTaskId = getCurrentTaskId(execution);
////            if (currentTaskId != null) {
////                taskService.setAssignee(currentTaskId, departmentHierarchy.get(currentLevel));
////            }
//        } else {
//            execution.setVariable("loopCondition", false);
//        }
//    }
//
// // Get current task ID
//    private String getCurrentTaskId(DelegateExecution execution) {
//        TaskService taskService = applicationContext.getBean(TaskService.class);
//        String processInstanceId = execution.getProcessInstanceId();
//        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
//        if (!tasks.isEmpty()) {
// return tasks.get(0).getId(); // assuming current task in only task
//        }
//        return null;
//    }
//}

package com.ruoyi.web.controller.delegate;

import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.impl.SysDeptServiceImpl;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.api.Task;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoopConditionDelegate implements JavaDelegate, ApplicationContextAware {
    private String excludedUdid;

    public LoopConditionDelegate(@Value("${dept.excludedUdid}") String excludedUdid) {
        this.excludedUdid = excludedUdid;
    }

    private static ApplicationContext applicationContext;

    public LoopConditionDelegate() {
        // No-args constructor
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        LoopConditionDelegate.applicationContext = applicationContext;
    }

    @Override
    public void execute(DelegateExecution execution) {
        SysUserServiceImpl sysUserServiceImpl = (SysUserServiceImpl) applicationContext.getBean("sysUserServiceImpl");
        SysDeptServiceImpl sysDeptServiceImpl = (SysDeptServiceImpl) applicationContext.getBean("sysDeptServiceImpl");
        // assuming department hierarchy
        String currentAssignee = (String) execution.getVariable("currentAssignee");
        List<SysUser> leaders = sysUserServiceImpl.getLeaders(currentAssignee);

//        Object nextUserIds = execution.getVariable("nextUserIds");
//        System.out.println("nextUserIds = " + nextUserIds);

//        if (CollectionUtils.isEmpty(leaders)) {
//            execution.setVariable("currentAssignee", null);
//        }
        // Set usernode assignee
//        execution.setVariable("currentAssignee", getCurrentTaskId(execution));

        // if leader, approvalfinish
        if (CollectionUtils.isEmpty(leaders)) {
            // Check if it is bureau leader
            SysUser sysUser = sysUserServiceImpl.selectUserById(currentAssignee);
            SysDeptView sysDeptView = sysDeptServiceImpl.selectDeptById(sysUser.getDeptId());
            if (!excludedUdid.equals(sysDeptView.getDeptId())) {
                // Get all bureau leader
                // Get bureau leader department ID
                List<SysUser> users = sysUserServiceImpl.selectUserByUdid(excludedUdid);
                String userIdsCommaSeparated = users.stream().map(SysUser::getUserId).collect(Collectors.joining(","));
                System.out.println("userIdsCommaSeparated = " + userIdsCommaSeparated);
                execution.setVariable("nextUserIds", userIdsCommaSeparated);
            } else {
                execution.setVariable("flowDirection", "end");
            }
        }
    }

    // Get task Process
    private String getCurrentTaskId(DelegateExecution execution) {
        TaskService taskService = applicationContext.getBean(TaskService.class);
        String processInstanceId = execution.getProcessInstanceId();
        List<Task> tasks = taskService.createTaskQuery().
            taskTenantId(execution.getTenantId()).
            processInstanceId(processInstanceId).
            list();
        if (!tasks.isEmpty()) {
            return tasks.get(0).getAssignee(); // assuming current task in only task
        }
        return null;
    }
}
