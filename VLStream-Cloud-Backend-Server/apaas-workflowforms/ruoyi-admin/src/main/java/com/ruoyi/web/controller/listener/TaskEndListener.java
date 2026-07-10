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

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class TaskEndListener implements TaskListener, ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TaskEndListener.applicationContext = applicationContext;
    }

//    private String excludedUdid;
//    public TaskEndListener(@Value("${dept.excludedUdid}") String excludedUdid) {
//
//        this.excludedUdid = excludedUdid;
//    }
    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("执行TaskEndListener监听器");
        SysUserServiceImpl sysUserServiceImpl = (SysUserServiceImpl)applicationContext.getBean("sysUserServiceImpl");
        SysDeptServiceImpl sysDeptServiceImpl = (SysDeptServiceImpl)applicationContext.getBean("sysDeptServiceImpl");
        Environment environment = (Environment)applicationContext.getBean("environment");
        RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
        // 获取流程实例 ID
        String processInstanceId = delegateTask.getProcessInstanceId();
        // 获取之前设置的流程变量
        String currentAssignee = delegateTask.getAssignee();
        // 将当前任务的候选人设置为currentAssignees
        if (StringUtils.isNotBlank(currentAssignee)) {
            // 将当前任务的候选人列表设置为候选用户
            List<SysUser> leaders = sysUserServiceImpl.getLeaders(currentAssignee);
            if(!CollectionUtils.isEmpty(leaders)) {
//                String nextUserIds = (String) delegateTask.getVariable("nextUserIds");
//                if(StringUtils.isNotBlank(nextUserIds)){ // 页面选择了审批人
//                    List<String> leaderIds = Arrays.asList(nextUserIds.split(","));
////                    delegateTask.addCandidateUsers(leaderIds);
//                }else { // 页面未选择审批人
//                    List<String> leaderIds = leaders.stream()
//                        .map(SysUser::getUserId)
//                        .collect(Collectors.toList());
////                    delegateTask.addCandidateUsers(leaderIds);
//                }
                delegateTask.setVariable("flowDirection", "continue");
            } else {
//                delegateTask.setVariable("flowDirection", "end");
                // 判断是不是局领导
                SysUser sysUser = sysUserServiceImpl.selectUserById(currentAssignee);
                SysDeptView sysDeptView = sysDeptServiceImpl.selectDeptById(sysUser.getDeptId());
                if(!environment.getProperty("dept.excludedUdid").equals(sysDeptView.getDeptId())) {
                    // 获取所有局领导
//                    Long parentDeptId = sysDept.getParentId();
                    String udid = environment.getProperty("dept.excludedUdid");
//                    SysDept s = sysDeptServiceImpl.selectDeptByUdid(udid);
//                    SysDept parentDept = sysDeptServiceImpl.selectDeptById(s.getParentId());
                    List<SysUser> users = sysUserServiceImpl.selectUserByUdid(udid);
                    String userIdsCommaSeparated = users.stream().map(SysUser::getUserId).collect(Collectors.joining(","));
//                    delegateTask.setVariable("nextUserIds", userIdsCommaSeparated);
                    runtimeService.setVariable(processInstanceId, "nextUserIds", userIdsCommaSeparated);
                }else {
                    delegateTask.setVariable("flowDirection", "end");
                }
            }
        } else {
            delegateTask.setVariable("flowDirection", "end");
        }
        System.out.println("flowDirection: " + delegateTask.getVariable("flowDirection"));
    }
}
