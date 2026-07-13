/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
 * 流程抄送Service业务层处理
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
     * 查询流程抄送
     *
     * @param copyId 流程抄送主键
     * @return 流程抄送
     */
    @Override
    public WfCopyVo queryById(Long copyId) {
        return baseMapper.selectVoById(copyId);
    }

    /**
     * 查询流程抄送列表
     *
     * @param bo      流程抄送
     * @param sysUser
     * @return 流程抄送
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
        // 如果 processQuery 指定获取全部综合分类
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
            // 使用ProcessInstanceQuery筛选出符合条件的流程实例ID
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
     * 查询流程抄送列表
     *
     * @param bo 流程抄送
     * @return 流程抄送
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
//            // 若抄送用户为空，则不需要处理，返回成功
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
//        //判断抄送用户是否需要消息推送
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
            // 若抄送用户为空，则不需要处理，返回成功
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
            // 获取分类
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId())
                .processDefinitionId(processDefinitionId)
                .singleResult();
            copy.setCategoryId(processDefinition.getCategory());
            copyList.add(copy);
            //判断抄送用户是否需要消息推送
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
