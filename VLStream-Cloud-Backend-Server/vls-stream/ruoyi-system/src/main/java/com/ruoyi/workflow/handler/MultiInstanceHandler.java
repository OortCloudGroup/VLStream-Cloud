package com.ruoyi.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.system.domain.SysUserRoleView;
import lombok.AllArgsConstructor;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 多实例处理类
 *
 * @author KonBAI
 */
@AllArgsConstructor
@Component("multiInstanceHandler")
public class MultiInstanceHandler {

    public Set<String> getUserIds(DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        if (ObjectUtil.isNotEmpty(flowElement) && flowElement instanceof UserTask) {
            return getUserIds((UserTask) flowElement);
        }
        return new LinkedHashSet<>();
    }

    public Set<String> getUserIds(UserTask userTask) {
        Set<String> candidateUserIds = new LinkedHashSet<>();
        String dataType = userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_DATA_TYPE);
        if (CollUtil.isNotEmpty(userTask.getCandidateUsers())
            && (ObjectUtil.isEmpty(dataType) || "USERS".equals(dataType))) {
            // 添加候选用户id
            candidateUserIds.addAll(userTask.getCandidateUsers());
        } else if (CollUtil.isNotEmpty(userTask.getCandidateGroups())) {
            // 获取组的ID，角色ID集合或部门ID集合
            List<String > groups = userTask.getCandidateGroups().stream()
                .map(item -> item.substring(4))
                .collect(Collectors.toList());
            List<String> userIds = new ArrayList<>();
            if ("ROLES".equals(dataType)) {
                // 通过角色id，获取所有用户id集合
                LambdaQueryWrapper<SysUserRoleView> lqw = Wrappers.lambdaQuery(SysUserRoleView.class).
                                                                  select(SysUserRoleView::getUserId).
                                                                  in(SysUserRoleView::getRoleId, groups);
                userIds = SimpleQuery.list(lqw, SysUserRoleView::getUserId);
            } else if ("DEPTS".equals(dataType)) {
                // 通过部门id，获取所有用户id集合
                LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery(SysUser.class).select(SysUser::getUserId).in(SysUser::getDeptId, groups);
                userIds = SimpleQuery.list(lqw, SysUser::getUserId);
            }
            // 添加候选用户id
            userIds.forEach(id -> candidateUserIds.add(String.valueOf(id)));
        }
        return candidateUserIds;
    }
}
