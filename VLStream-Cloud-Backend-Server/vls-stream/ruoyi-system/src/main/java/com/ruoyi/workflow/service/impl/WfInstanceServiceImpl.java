/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.common.constant.TaskConstants;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.system.domain.SysUserRoleView;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.domain.vo.WfFormVo;
import com.ruoyi.workflow.domain.vo.WfTaskVo;
import com.ruoyi.workflow.service.IWfDeployFormService;
import com.ruoyi.workflow.service.IWfInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * workflow instance
 *
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WfInstanceServiceImpl extends FlowServiceFactory implements IWfInstanceService {

    private final IWfDeployFormService deployFormService;
    private final UserService userService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;

    /**
     * finishworkflow instance
     *
     * @param vo
     */
    @Override
    public void stopProcessInstance(WfTaskBo vo) {
        String taskId = vo.getTaskId();
    }

    /**
     * workflow instance
     *
     * @param state
     * @param instanceId workflow instance ID
     */
    @Override
    public void updateState(Integer state, String instanceId) {
        //
        if (state == 1) {
            runtimeService.activateProcessInstanceById(instanceId);
        }
        //
        if (state == 2) {
            runtimeService.suspendProcessInstanceById(instanceId);
        }
    }

    /**
     * Delete workflow instance ID
     *
     * @param instanceId workflow instance ID
     * @param deleteReason Delete
     * @param sysUser
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String instanceId, String deleteReason, SysUser sysUser) {
        // Query history data
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceById(instanceId,sysUser);
        if (historicProcessInstance.getEndTime() != null) {
            historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
            return;
        }
        // Delete workflow instance
        runtimeService.deleteProcessInstance(instanceId, deleteReason);
        // Delete history workflow instance
        historyService.deleteHistoricProcessInstance(instanceId);
    }

    /**
     * instanceIDQuery history instancedata
     *
     * @param processInstanceId
     * @return
     */
    @Override
    public HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId,  SysUser sysUser) {
        HistoricProcessInstance historicProcessInstance =
                historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId())
                    .processInstanceId(processInstanceId).singleResult();
        if (Objects.isNull(historicProcessInstance)) {
            throw new FlowableObjectNotFoundException("流程实例不存在: " + processInstanceId);
        }
        return historicProcessInstance;
    }


    /**
     * workflowhistory record
     *
     * @param procInsId workflow instanceId
     * @return
     */
    @Override
    public Map<String, Object> queryDetailProcess(String procInsId, String deployId) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotBlank(procInsId)) {
            List<HistoricTaskInstance> taskInstanceList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(procInsId)
                .orderByHistoricTaskInstanceStartTime().desc()
                .list();
            List<Comment> commentList = taskService.getProcessInstanceComments(procInsId);
            List<WfTaskVo> taskVoList = new ArrayList<>(taskInstanceList.size());
            taskInstanceList.forEach(taskInstance -> {
                WfTaskVo taskVo = new WfTaskVo();
                taskVo.setProcDefId(taskInstance.getProcessDefinitionId());
                taskVo.setTaskId(taskInstance.getId());
                taskVo.setTaskDefKey(taskInstance.getTaskDefinitionKey());
                taskVo.setTaskName(taskInstance.getName());
                taskVo.setCreateTime(taskInstance.getStartTime());
                taskVo.setFinishTime(taskInstance.getEndTime());
                if (StringUtils.isNotBlank(taskInstance.getAssignee())) {
                    String userId = taskInstance.getAssignee();
                    String userName = userService.selectUserNameById(userId);
                    taskVo.setAssigneeId(userId);
                    taskVo.setAssigneeName(userName);
                }
                // approver
                List<HistoricIdentityLink> linksForTask = historyService.getHistoricIdentityLinksForTask(taskInstance.getId());
                StringBuilder stringBuilder = new StringBuilder();
                for (HistoricIdentityLink identityLink : linksForTask) {
                    if ("candidate".equals(identityLink.getType())) {
                        if (StringUtils.isNotBlank(identityLink.getUserId())) {
                            String userId = identityLink.getUserId();
                            String userName = userService.selectUserNameById(userId);
                            stringBuilder.append(userName).append(",");
                        }
                        if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                            if (identityLink.getGroupId().startsWith(TaskConstants.ROLE_GROUP_PREFIX)) {
                                String roleId = StringUtils.stripStart(identityLink.getGroupId(), TaskConstants.ROLE_GROUP_PREFIX);
                                SysUserRoleView role = roleService.selectRoleById(roleId);
                                stringBuilder.append(role.getRoleName()).append(",");
                            } else if (identityLink.getGroupId().startsWith(TaskConstants.DEPT_GROUP_PREFIX)) {
                                String deptId = StringUtils.stripStart(identityLink.getGroupId(), TaskConstants.DEPT_GROUP_PREFIX);
                                SysDeptView dept = deptService.selectDeptById(deptId);
                                stringBuilder.append(dept.getDeptName()).append(",");
                            }
                        }
                    }
                }
                if (StringUtils.isNotBlank(stringBuilder)) {
                    taskVo.setCandidate(stringBuilder.substring(0, stringBuilder.length() - 1));
                }
                if (ObjectUtil.isNotNull(taskInstance.getDurationInMillis())) {
                    taskVo.setDuration(DateUtil.formatBetween(taskInstance.getDurationInMillis(), BetweenFormatter.Level.SECOND));
                }
                // Get
                if (CollUtil.isNotEmpty(commentList)) {
                    List<Comment> comments = new ArrayList<>();
                    // commentList.stream().filter(comment -> taskInstance.getId().equals(comment.getTaskId())).collect(Collectors.toList());
                    for (Comment comment : commentList) {
                        if (comment.getTaskId().equals(taskInstance.getId())) {
                            comments.add(comment);
                            // taskVo.setComment(WfCommentDto.builder().type(comment.getType()).comment(comment.getFullMessage()).build());
                        }
                    }
                    taskVo.setCommentList(comments);
                }
                taskVoList.add(taskVo);
            });
            map.put("flowList", taskVoList);
// // Query current taskwhether
//            List<Task> taskList = taskService.createTaskQuery()
//            .taskTenantId(execution.getTenantId())
//            .processInstanceId(procInsId).list();
//            if (CollectionUtils.isNotEmpty(taskList)) {
//                map.put("finished", true);
//            } else {
//                map.put("finished", false);
//            }
        }
        // Get Initialize form
        if (StringUtils.isNotBlank(deployId)) {
            WfFormVo formVo = deployFormService.selectDeployFormByDeployId(deployId);
            if (Objects.isNull(formVo)) {
                throw new ServiceException("请先配置流程表单");
            }
            map.put("formData", JsonUtils.parseObject(formVo.getContent(), Map.class));
        }
        return map;
    }
}
