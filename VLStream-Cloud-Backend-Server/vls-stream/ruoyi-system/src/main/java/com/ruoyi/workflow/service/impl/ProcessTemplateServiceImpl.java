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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.ProcessTemplate;
import com.ruoyi.workflow.domain.bo.ProcessTemplateBo;
import com.ruoyi.workflow.domain.vo.ProcessTemplateVo;
import com.ruoyi.workflow.mapper.ProcessTemplateMapper;
import com.ruoyi.workflow.service.IProcessTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * workflowInitialize Service layer Process
 *
 * @author lcq
 * @date 2025-01-07
 */
@RequiredArgsConstructor
@Service
public class ProcessTemplateServiceImpl implements IProcessTemplateService {

    private final ProcessTemplateMapper baseMapper;

    /**
     * Query workflowInitialize
     */
    @Override
    public ProcessTemplateVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * Query workflowInitialize list
     */
    @Override
    public TableDataInfo<ProcessTemplateVo> queryPageList(ProcessTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProcessTemplate> lqw = buildQueryWrapper(bo);
        Page<ProcessTemplateVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * Query workflowInitialize list
     */
    @Override
    public List<ProcessTemplateVo> queryList(ProcessTemplateBo bo) {
        LambdaQueryWrapper<ProcessTemplate> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ProcessTemplate> buildQueryWrapper(ProcessTemplateBo bo) {
        if (bo == null) {
            return null;
        }
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ProcessTemplate> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getDeploymentId()), ProcessTemplate::getDeploymentId, bo.getDeploymentId());
        lqw.eq(StringUtils.isNotBlank(bo.getModelId()), ProcessTemplate::getModelId, bo.getModelId());
        lqw.eq(StringUtils.isNotBlank(bo.getModelKey()), ProcessTemplate::getModelKey, bo.getModelKey());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ProcessTemplate::getModelName, bo.getModelName());
        lqw.eq(StringUtils.isNotBlank(bo.getShowMobile()), ProcessTemplate::getShowMobile, bo.getShowMobile());
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), ProcessTemplate::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getUserId()), ProcessTemplate::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), ProcessTemplate::getDescription, bo.getDescription());
        return lqw;
    }

    /**
     * Add workflowInitialize
     */
    @Override
    public Boolean insertByBo(ProcessTemplateBo bo) {
        ProcessTemplate add = BeanUtil.toBean(bo, ProcessTemplate.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * Update workflowInitialize
     */
    @Override
    public Boolean updateByBo(ProcessTemplateBo bo) {
        ProcessTemplate update = BeanUtil.toBean(bo, ProcessTemplate.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     */
    private void validEntityBeforeSave(ProcessTemplate entity) {
        // TODO dataValidate ,
    }

    /**
     * Batch delete workflowInitialize
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO Validate ,Check whether need to Validate
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
