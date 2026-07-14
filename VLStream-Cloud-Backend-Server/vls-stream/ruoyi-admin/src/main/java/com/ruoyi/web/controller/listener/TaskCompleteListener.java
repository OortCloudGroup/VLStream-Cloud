/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.listener;

import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.impl.SysDeptServiceImpl;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class TaskCompleteListener implements TaskListener, ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TaskCompleteListener.applicationContext = applicationContext;
    }
    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("执行TaskCompleteListener监听器");
        SysUserServiceImpl sysUserServiceImpl = (SysUserServiceImpl)applicationContext.getBean("sysUserServiceImpl");
        SysDeptServiceImpl sysDeptServiceImpl = (SysDeptServiceImpl)applicationContext.getBean("sysDeptServiceImpl");
        RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
        // 获取执行对象
        String executionId = delegateTask.getExecutionId();
//        Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();

        // 获取流程实例 ID
        String processInstanceId = delegateTask.getProcessInstanceId();
        // 获取全局变量
        String nextUserIds = (String) runtimeService.getVariable(processInstanceId, "nextUserIds");
        Environment environment = (Environment)applicationContext.getBean("environment");
        // 获取之前设置的流程变量
        String currentAssignee = (String) delegateTask.getVariable("currentAssignee");
        String nex = (String) delegateTask.getVariable("nextUserIds");
        // 将当前任务的候选人设置为currentAssignees
        if (StringUtils.isNotBlank(currentAssignee)) {
            // 将当前任务的候选人列表设置为候选用户
            List<SysUser> leaders = sysUserServiceImpl.getLeaders(currentAssignee);
            if(!CollectionUtils.isEmpty(leaders) ) {
                if(StringUtils.isNotBlank(nextUserIds)){ // 页面选择了审批人
                    List<String> leaderIds = Arrays.asList(nextUserIds.split(","));
                    delegateTask.addCandidateUsers(leaderIds);
                }else { // 页面未选择审批人
                    List<String> leaderIds = leaders.stream()
                        .map(SysUser::getUserId)
                        .collect(Collectors.toList());
                    delegateTask.addCandidateUsers(leaderIds);
                }
                delegateTask.setVariable("flowDirection", "continue");
            } else {
                SysUser sysUser = sysUserServiceImpl.selectUserById(currentAssignee);
                SysDeptView sysDeptView = sysDeptServiceImpl.selectDeptById(sysUser.getDeptId());
                // 判断是不是局领导
                if(!environment.getProperty("dept.excludedUdid").equals(sysDeptView.getDeptId())) {
                    // 获取所有局领导
                    // 先获取局领导的部门id
//                    Long parentDeptId = sysDept.getParentId();
                    String udid = environment.getProperty("dept.excludedUdid");
                    List<SysUser> users = sysUserServiceImpl.selectUserByUdid(udid);
                    String userIdsCommaSeparated = users.stream().map(SysUser::getUserId).collect(Collectors.joining(","));
//                    delegateTask.setVariable("nextUserIds", userIdsCommaSeparated);
                    runtimeService.setVariable(processInstanceId, "nextUserIds", userIdsCommaSeparated);
                    List<String> collect = users.stream().map(SysUser::getUserId).collect(Collectors.toList());
                    delegateTask.addCandidateUsers(collect);
                }else {
                    delegateTask.setVariable("flowDirection", "end");
                }
            }
//            delegateTask.setVariable("nextUserIds", null);
        } else {
            delegateTask.setVariable("flowDirection", "end");
        }
        System.out.println("flowDirection: " + delegateTask.getVariable("flowDirection"));
    }
}
