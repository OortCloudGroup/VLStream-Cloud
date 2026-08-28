/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.bo.WfSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;
import com.ruoyi.workflow.mapper.WfSynthesisMapper;
import com.ruoyi.workflow.service.IWfSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * workflowService layer Process
 *
 * @author
 * @date 2025-01-04
 */
@RequiredArgsConstructor
@Service
public class WfSynthesisServiceImpl extends ServiceImpl<WfSynthesisMapper, WfSynthesis> implements IWfSynthesisService {

    @Resource
    WfSynthesisMapper baseMapper;
    private final ValidateService validateService;


    /**
     * Query workflow
     */
    @Override
    public WfSynthesisVo queryById(String synthesisId) {
        return baseMapper.selectVoById(synthesisId);
    }


    /**
     * Query workflow list
     */
    @Override
    public List<WfSynthesisVo> queryList(WfSynthesisBo bo) {
        LambdaQueryWrapper<WfSynthesis> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<WfSynthesisVo> queryListAll(String categoryName) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<WfSynthesis>().eq(StringUtils.isNotBlank(categoryName), WfSynthesis::getCategoryName, categoryName));
    }


    private LambdaQueryWrapper<WfSynthesis> buildQueryWrapper(WfSynthesisBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfSynthesis> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), WfSynthesis::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), WfSynthesis::getCategoryName, bo.getCategoryName());
        if (StringUtils.isBlank(bo.getSynthesisId()) && StringUtils.isBlank(bo.getParentId())) {
            lqw.isNull(WfSynthesis::getParentId);  // if bo.getId() and bo.getParentId() is empty, Query parent_id to NULL
        }
        lqw.eq(WfSynthesis::getDelFlag,"0");
        return lqw;
    }

    /**
     * Add workflow
     */
    @Override
    public Boolean insertByBo(WfSynthesisBo bo) {
        WfSynthesis add = BeanUtil.toBean(bo, WfSynthesis.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setSynthesisId(add.getSynthesisId());
        }
        return flag;
    }

    /**
     * Update workflow
     */
    @Override
    public Boolean updateByBo(WfSynthesisBo bo) {
        WfSynthesis update = BeanUtil.toBean(bo, WfSynthesis.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     */
    private void validEntityBeforeSave(WfSynthesis entity) {
        // TODO dataValidate ,
    }

    /**
     * Batch delete workflow
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
}
