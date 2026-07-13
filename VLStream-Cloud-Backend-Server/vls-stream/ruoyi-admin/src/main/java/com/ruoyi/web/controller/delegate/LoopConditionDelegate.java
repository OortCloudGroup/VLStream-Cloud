/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
//        // 无参构造函数
//    }
//    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
//        this.applicationContext = arg0;
//    }
//    @Override
//    public void execute(DelegateExecution execution) {
//        // 假设我们有一个部门层级的列表
//        List<String> departmentHierarchy = (List<String>) execution.getVariable("departmentHierarchy");
//        Integer currentLevel = (Integer) execution.getVariable("currentLevel");
//
//        // 检查是否还有下一个层级
//        if (currentLevel < departmentHierarchy.size() - 1) {
//            currentLevel++;
//            execution.setVariable("currentAssignee", departmentHierarchy.get(currentLevel));
//            execution.setVariable("currentLevel", currentLevel);
//            execution.setVariable("loopCondition", true);
//
//            TaskService taskService = (TaskService)applicationContext.getBean(TaskService.class);
////            // 设置当前任务的处理人
////            String currentTaskId = getCurrentTaskId(execution);
////            if (currentTaskId != null) {
////                taskService.setAssignee(currentTaskId, departmentHierarchy.get(currentLevel));
////            }
//        } else {
//            execution.setVariable("loopCondition", false);
//        }
//    }
//
//    // 获取当前任务的 ID
//    private String getCurrentTaskId(DelegateExecution execution) {
//        TaskService taskService = applicationContext.getBean(TaskService.class);
//        String processInstanceId = execution.getProcessInstanceId();
//        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
//        if (!tasks.isEmpty()) {
//            return tasks.get(0).getId(); // 假设当前任务列表中只有一个任务
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
        // 无参构造函数
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        LoopConditionDelegate.applicationContext = applicationContext;
    }

    @Override
    public void execute(DelegateExecution execution) {
        SysUserServiceImpl sysUserServiceImpl = (SysUserServiceImpl) applicationContext.getBean("sysUserServiceImpl");
        SysDeptServiceImpl sysDeptServiceImpl = (SysDeptServiceImpl) applicationContext.getBean("sysDeptServiceImpl");
        // 假设我们有一个部门层级的列表
        String currentAssignee = (String) execution.getVariable("currentAssignee");
        List<SysUser> leaders = sysUserServiceImpl.getLeaders(currentAssignee);

//        Object nextUserIds = execution.getVariable("nextUserIds");
//        System.out.println("nextUserIds = " + nextUserIds);

//        if (CollectionUtils.isEmpty(leaders)) {
//            execution.setVariable("currentAssignee", null);
//        }
        // 设置上一个用户节点的实际办理人
//        execution.setVariable("currentAssignee", getCurrentTaskId(execution));

        // 如果没有领导，则审批结束
        if (CollectionUtils.isEmpty(leaders)) {
            // 判断是不是局领导
            SysUser sysUser = sysUserServiceImpl.selectUserById(currentAssignee);
            SysDeptView sysDeptView = sysDeptServiceImpl.selectDeptById(sysUser.getDeptId());
            if (!excludedUdid.equals(sysDeptView.getDeptId())) {
                // 获取所有局领导
                // 先获取局领导的部门id
                List<SysUser> users = sysUserServiceImpl.selectUserByUdid(excludedUdid);
                String userIdsCommaSeparated = users.stream().map(SysUser::getUserId).collect(Collectors.joining(","));
                System.out.println("userIdsCommaSeparated = " + userIdsCommaSeparated);
                execution.setVariable("nextUserIds", userIdsCommaSeparated);
            } else {
                execution.setVariable("flowDirection", "end");
            }
        }
    }

    // 获取上个任务的 处理人
    private String getCurrentTaskId(DelegateExecution execution) {
        TaskService taskService = applicationContext.getBean(TaskService.class);
        String processInstanceId = execution.getProcessInstanceId();
        List<Task> tasks = taskService.createTaskQuery().
            taskTenantId(execution.getTenantId()).
            processInstanceId(processInstanceId).
            list();
        if (!tasks.isEmpty()) {
            return tasks.get(0).getAssignee(); // 假设当前任务列表中只有一个任务
        }
        return null;
    }
}
