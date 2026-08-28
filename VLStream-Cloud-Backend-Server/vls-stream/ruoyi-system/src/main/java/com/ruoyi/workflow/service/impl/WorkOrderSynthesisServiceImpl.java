/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.workflow.domain.WorkOrderSynthesis;
import com.ruoyi.workflow.domain.bo.WorkOrderSynthesisBo;
import com.ruoyi.workflow.domain.vo.WorkOrderSynthesisVo;
import com.ruoyi.workflow.mapper.WorkOrderSynthesisMapper;
import com.ruoyi.workflow.service.IWorkOrderSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * work orderworkflowService layer Process
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
@RequiredArgsConstructor
@Service
public class WorkOrderSynthesisServiceImpl extends ServiceImpl<WorkOrderSynthesisMapper, WorkOrderSynthesis> implements IWorkOrderSynthesisService {

    private final WorkOrderSynthesisMapper baseMapper;
    private final ValidateService validateService;


    /**
     * Query work orderworkflow
     */
    @Override
    public WorkOrderSynthesisVo queryById(String synthesisId) {
        return baseMapper.selectVoById(synthesisId);
    }


    /**
     * Query work orderworkflow list
     */
    @Override
    public List<WorkOrderSynthesisVo> queryList(WorkOrderSynthesisBo bo) {
        LambdaQueryWrapper<WorkOrderSynthesis> lqw = buildQueryWrapper(bo);
        List<WorkOrderSynthesisVo> workOrderSynthesisVos = baseMapper.selectVoList(lqw);
        // recordwhether sub , work order need to
        for (WorkOrderSynthesisVo workOrderSynthesisVo : workOrderSynthesisVos) {
            Long l = baseMapper.selectCount(new LambdaQueryWrapper<WorkOrderSynthesis>().eq(WorkOrderSynthesis::getParentId, workOrderSynthesisVo.getSynthesisId()));
            workOrderSynthesisVo.setChildFlag(l > 0);
        }
        return workOrderSynthesisVos;
    }

    private LambdaQueryWrapper<WorkOrderSynthesis> buildQueryWrapper(WorkOrderSynthesisBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WorkOrderSynthesis> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), WorkOrderSynthesis::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), WorkOrderSynthesis::getCategoryName, bo.getCategoryName());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), WorkOrderSynthesis::getDescription, bo.getDescription());
        if (StringUtils.isBlank(bo.getSynthesisId()) && StringUtils.isBlank(bo.getParentId())) {
            lqw.isNull(WorkOrderSynthesis::getParentId);  // if bo.getId() and bo.getParentId() is empty, Query parent_id to NULL
        }
        return lqw;
    }

    /**
     * Add work orderworkflow
     */
    @Override
    public Boolean insertByBo(WorkOrderSynthesisBo bo) {
        WorkOrderSynthesis add = BeanUtil.toBean(bo, WorkOrderSynthesis.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setSynthesisId(add.getSynthesisId());
        }
        return flag;
    }

    /**
     * Update work orderworkflow
     */
    @Override
    public Boolean updateByBo(WorkOrderSynthesisBo bo) {
        WorkOrderSynthesis update = BeanUtil.toBean(bo, WorkOrderSynthesis.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     */
    private void validEntityBeforeSave(WorkOrderSynthesis entity) {
        // TODO dataValidate ,
    }

    /**
     * Batch delete work orderworkflow
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // whether data
            validateService.validateBeforeDeletion(ids);
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<String> selectChildById(String parentId) {
        return baseMapper.selectChildById(parentId);
    }

    @Override
    public List<WorkOrderSynthesisVo> queryListAll(String categoryName) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<WorkOrderSynthesis>().eq(StringUtils.isNotBlank(categoryName), WorkOrderSynthesis::getCategoryName, categoryName));
    }
}
