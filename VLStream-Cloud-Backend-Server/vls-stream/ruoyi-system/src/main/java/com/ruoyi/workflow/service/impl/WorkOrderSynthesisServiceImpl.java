/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
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
 * 综合工单流程Service业务层处理
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
     * 查询综合工单流程
     */
    @Override
    public WorkOrderSynthesisVo queryById(String synthesisId) {
        return baseMapper.selectVoById(synthesisId);
    }


    /**
     * 查询综合工单流程列表
     */
    @Override
    public List<WorkOrderSynthesisVo> queryList(WorkOrderSynthesisBo bo) {
        LambdaQueryWrapper<WorkOrderSynthesis> lqw = buildQueryWrapper(bo);
        List<WorkOrderSynthesisVo> workOrderSynthesisVos = baseMapper.selectVoList(lqw);
        //记录是否有子集，发起工单需要用到
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
            lqw.isNull(WorkOrderSynthesis::getParentId);  // 如果bo.getId()和bo.getParentId()都为空，查询parent_id为NULL
        }
        return lqw;
    }

    /**
     * 新增综合工单流程
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
     * 修改综合工单流程
     */
    @Override
    public Boolean updateByBo(WorkOrderSynthesisBo bo) {
        WorkOrderSynthesis update = BeanUtil.toBean(bo, WorkOrderSynthesis.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WorkOrderSynthesis entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除综合工单流程
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //效验这个分类下是否还有别的数据
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
