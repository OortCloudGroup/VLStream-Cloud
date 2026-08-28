/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.WfCopy;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.bo.WfCopyBo;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.domain.vo.WfCopyVo;
import com.ruoyi.workflow.mapper.WfCopyMapper;
import com.ruoyi.workflow.service.IWfAppService;
import com.ruoyi.workflow.service.IWfCopyService;
import com.ruoyi.workflow.service.IWfSynthesisService;
import com.ruoyi.workflow.service.IWfTaskService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * workflow Service layer Process
 *
 * @author KonBAI
 * @date 2022-05-19
 */
@RequiredArgsConstructor
@Service
public class WfCopyServiceImpl extends FlowServiceFactory implements IWfCopyService {

    private final WfCopyMapper baseMapper;
    private final IWfAppService wfAppService;
    private final IWfSynthesisService wfSynthesisService;
    @Lazy
    @Resource
    IWfTaskService wfTaskService;
    private final SysUserServiceImpl sysUserServiceImpl;

    /**
     * Query workflow
     *
     * @param copyId workflow primary key
     * @return workflow
     */
    @Override
    public WfCopyVo queryById(Long copyId) {
        return baseMapper.selectVoById(copyId);
    }

    /**
     * Query workflow list
     *
     * @param bo workflow
     * @param sysUser
     * @return workflow
     */
    @Override
    public TableDataInfo<WfCopyVo> selectPageList(WfCopyBo bo, PageQuery pageQuery, SysUser sysUser) {
        bo.setUserId(sysUser.getUserId());
        LambdaQueryWrapper<WfCopy> lqw = buildQueryWrapper(bo);
        lqw.eq(StringUtils.isNotBlank(bo.getCategoryId()), WfCopy::getCategoryId, bo.getCategoryId());
        if (Boolean.TRUE.equals(bo.getWfAppAll())) {
            List<String> appList = wfAppService.list()
                                               .stream()
                                               .map(WfApp::getAppId)
                                               .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                lqw.in(WfCopy::getCategoryId, appList);
            }
        }
        // if processQuery Get full
        else if (Boolean.TRUE.equals(bo.getWfSynthesisAll())) {
            List<String> wfSynthesisList = wfSynthesisService.list()
                                                             .stream()
                                                             .map(WfSynthesis::getSynthesisId)
                                                             .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
                lqw.in(WfCopy::getCategoryId, wfSynthesisList);
            }
        }
        if (ObjectUtil.isNotNull(bo.getProStartBeginTime()) && ObjectUtil.isNotNull(bo.getProStartEndTime())) {
            Date instanceBeginTime = bo.getProStartBeginTime();
            Date instanceEndTime = bo.getProStartEndTime();
            // ProcessInstanceQuery workflow instance ID
            ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .startedAfter(instanceBeginTime)
                .startedBefore(instanceEndTime);
            List<String> instanceIds = processInstanceQuery.list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(instanceIds)) {
                instanceIds.add(String.valueOf(UUID.randomUUID()));
            }
            lqw.in(WfCopy::getInstanceId, instanceIds);
        }
        lqw.orderByDesc(WfCopy::getCreateTime);
        Page<WfCopyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<WfCopyVo> records = result.getRecords();
        for (WfCopyVo wfCopyVo : records) {
            String instanceId = wfCopyVo.getInstanceId();
            System.out.println("instanceId = " + instanceId);
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceId(instanceId)
                .singleResult();
            wfCopyVo.setProInsCreateTime(historicProcessInstance.getStartTime());
        }
        return TableDataInfo.build(result);
    }

    /**
     * Query workflow list
     *
     * @param bo workflow
     * @return workflow
     */
    @Override
    public List<WfCopyVo> selectList(WfCopyBo bo) {
        LambdaQueryWrapper<WfCopy> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WfCopy> buildQueryWrapper(WfCopyBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfCopy> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, WfCopy::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getProcessName()), WfCopy::getProcessName, bo.getProcessName());
        lqw.like(StringUtils.isNotBlank(bo.getOriginatorName()), WfCopy::getOriginatorName, bo.getOriginatorName());
        return lqw;
    }

//    @Override
//    public Boolean makeCopy(WfTaskBo taskBo, SysUser sysUser) {
//        if (StringUtils.isBlank(taskBo.getCopyUserIds())) {
// // user is empty, need to Process , successfully
//            return true;
//        }
//        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
//            .processInstanceTenantId(sysUser.getTenantId())
//            .processInstanceId(taskBo.getProcInsId()).singleResult();
//        String[] ids = taskBo.getCopyUserIds().split(",");
//        List<WfCopy> copyList = new ArrayList<>(ids.length);
//        String originatorId = null;
////        String originatorId = LoginHelper.getUserId();
//        String originatorName = LoginHelper.getUsername();
//        String title = historicProcessInstance.getProcessDefinitionName() + "-" + taskBo.getTaskName();
//        for (String id : ids) {
//            WfCopy copy = new WfCopy();
//            copy.setTitle(title);
//            copy.setProcessId(historicProcessInstance.getProcessDefinitionId());
//            copy.setProcessName(historicProcessInstance.getProcessDefinitionName());
//            copy.setDeploymentId(historicProcessInstance.getDeploymentId());
//            copy.setInstanceId(taskBo.getProcInsId());
//            copy.setTaskId(taskBo.getTaskId());
//            copy.setUserId(id);
//            copy.setOriginatorId(originatorId);
//            copy.setOriginatorName(originatorName);
//            copyList.add(copy);
//        }
//
// //Check userwhether need to Push
//        if (taskBo.isPushMessage()) {
////            wfTaskService.sendMessage(true,taskBo.getCopyUserIds());
//            Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).processInstanceId(taskBo.getProcInsId()).orderByTaskCreateTime().desc().singleResult();
//            wfTaskService.buildAndSendUnifiedMessage(task,taskBo.getCopyUserIds(),true,sysUser);
//        }
//        return baseMapper.insertBatch(copyList);
//    }

    private SysUser getSysUser(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if (user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        System.out.println(" 用户缓存信息 " + user);
        return user;
    }

    public Boolean makeCopy(WfTaskBo taskBo, SysUser sysUser) {
        if (StringUtils.isBlank(taskBo.getCopyUserIds())) {
            // user is empty, need to Process , successfully
            return true;
        }
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceTenantId(sysUser.getTenantId())
            .processInstanceId(taskBo.getProcInsId()).singleResult();
        String[] ids = taskBo.getCopyUserIds().split(",");
        List<WfCopy> copyList = new ArrayList<>(ids.length);

        String originatorId = sysUser.getUserId();
        String originatorName = sysUser.getUserName();
        String title = historicProcessInstance.getProcessDefinitionName() + "-" + taskBo.getTaskName();
        for (String id : ids) {
            WfCopy copy = new WfCopy();
            copy.setTitle(title);
            copy.setProcessId(historicProcessInstance.getProcessDefinitionId());
            copy.setProcessName(historicProcessInstance.getProcessDefinitionName());
            copy.setDeploymentId(historicProcessInstance.getDeploymentId());
            copy.setInstanceId(taskBo.getProcInsId());
            copy.setTaskId(taskBo.getTaskId());
            copy.setUserId(id);
            copy.setOriginatorId(originatorId);
            copy.setOriginatorName(originatorName);
            // Get
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId())
                .processDefinitionId(processDefinitionId)
                .singleResult();
            copy.setCategoryId(processDefinition.getCategory());
            copyList.add(copy);
            // Check userwhether need to Push
//            if (taskBo.isPushMessage()) {
//            wfTaskService.sendMessage(true, taskBo.getCopyUserIds());
//                Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).processInstanceId(taskBo.getProcInsId()).orderByTaskCreateTime().desc().singleResult();
//                wfTaskService.buildAndSendUnifiedMessage(task,id,true,sysUser);
//            }
        }
        return baseMapper.insertBatch(copyList);
    }

    @Override
    public List<String> selectCopyUserIdByTaskId(String taskId) {
        LambdaQueryWrapper<WfCopy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCopy::getTaskId, taskId);
        List<WfCopy> wfCopyList = baseMapper.selectList(wrapper);

        List<String> userIdList = new ArrayList<>();
        for (WfCopy wfCopy : wfCopyList) {
            userIdList.add(wfCopy.getUserId());
        }

        return userIdList;
    }
}
