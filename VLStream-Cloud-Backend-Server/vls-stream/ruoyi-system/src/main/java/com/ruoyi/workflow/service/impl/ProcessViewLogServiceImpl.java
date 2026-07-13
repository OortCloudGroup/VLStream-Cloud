/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.ProcessViewLog;
import com.ruoyi.workflow.domain.bo.ProcessViewLogBo;
import com.ruoyi.workflow.domain.vo.ProcessViewLogVo;
import com.ruoyi.workflow.mapper.ProcessViewLogMapper;
import com.ruoyi.workflow.service.IProcessViewLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 流程访问日志Service业务层处理
 *
 * @author lcq
 * @date 2025-08-15
 */
@RequiredArgsConstructor
@Service
public class ProcessViewLogServiceImpl implements IProcessViewLogService {

    private final ProcessViewLogMapper baseMapper;

    /**
     * 查询流程访问日志
     */
    @Override
    public ProcessViewLogVo queryById(String id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询流程访问日志列表
     */
    @Override
    public TableDataInfo<ProcessViewLogVo> queryPageList(ProcessViewLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProcessViewLog> lqw = buildQueryWrapper(bo);
        Page<ProcessViewLogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询流程访问日志列表
     */
    @Override
    public List<ProcessViewLogVo> queryList(ProcessViewLogBo bo) {
        LambdaQueryWrapper<ProcessViewLog> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ProcessViewLog> buildQueryWrapper(ProcessViewLogBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ProcessViewLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getProcessInstanceId()), ProcessViewLog::getProcessInstanceId, bo.getProcessInstanceId());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessKey()), ProcessViewLog::getProcessKey, bo.getProcessKey());
        lqw.eq(StringUtils.isNotBlank(bo.getViewerUserId()), ProcessViewLog::getViewerUserId, bo.getViewerUserId());
        lqw.like(StringUtils.isNotBlank(bo.getViewerUsername()), ProcessViewLog::getViewerUsername, bo.getViewerUsername());
        lqw.eq(StringUtils.isNotBlank(bo.getViewerDeptId()), ProcessViewLog::getViewerDeptId, bo.getViewerDeptId());
        lqw.like(StringUtils.isNotBlank(bo.getViewerDeptName()), ProcessViewLog::getViewerDeptName, bo.getViewerDeptName());
        lqw.eq(StringUtils.isNotBlank(bo.getOperationType()), ProcessViewLog::getOperationType, bo.getOperationType());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessStatus()), ProcessViewLog::getProcessStatus, bo.getProcessStatus());
        lqw.eq(bo.getViewTime() != null, ProcessViewLog::getViewTime, bo.getViewTime());
        lqw.like(StringUtils.isNotBlank(bo.getAttachmentName()), ProcessViewLog::getAttachmentName, bo.getAttachmentName());
        lqw.eq(StringUtils.isNotBlank(bo.getDelFlag()), ProcessViewLog::getDelFlag,"0");
        return lqw;
    }

    /**
     * 新增流程访问日志
     */
    @Override
    public Boolean insertByBo(ProcessViewLogBo bo, SysUser sysUser) {
        ProcessViewLog add = BeanUtil.toBean(bo, ProcessViewLog.class);
        add.setViewerUserId(sysUser.getUserId());
        add.setViewerUsername(sysUser.getUserName());
        add.setViewerDeptId(sysUser.getDeptId());
        add.setViewerDeptName(sysUser.getDeptName());
        add.setViewTime(new Date());
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改流程访问日志
     */
    @Override
    public Boolean updateByBo(ProcessViewLogBo bo) {
        ProcessViewLog update = BeanUtil.toBean(bo, ProcessViewLog.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ProcessViewLog entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除流程访问日志
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public TableDataInfo<ProcessViewLogVo> queryUserPageList(ProcessViewLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProcessViewLog> lqw = buildQueryWrapper(bo);
        Page<ProcessViewLogVo> page = pageQuery.build();
        IPage<ProcessViewLogVo> result = baseMapper.selectLastVisitPerUserPage(page, lqw);
        return TableDataInfo.build(result);
    }


}
