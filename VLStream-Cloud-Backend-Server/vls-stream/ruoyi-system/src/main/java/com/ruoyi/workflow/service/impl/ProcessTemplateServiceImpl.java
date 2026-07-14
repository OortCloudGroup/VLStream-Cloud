/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 流程初始化模版Service业务层处理
 *
 * @author lcq
 * @date 2025-01-07
 */
@RequiredArgsConstructor
@Service
public class ProcessTemplateServiceImpl implements IProcessTemplateService {

    private final ProcessTemplateMapper baseMapper;

    /**
     * 查询流程初始化模版
     */
    @Override
    public ProcessTemplateVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询流程初始化模版列表
     */
    @Override
    public TableDataInfo<ProcessTemplateVo> queryPageList(ProcessTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProcessTemplate> lqw = buildQueryWrapper(bo);
        Page<ProcessTemplateVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询流程初始化模版列表
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
     * 新增流程初始化模版
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
     * 修改流程初始化模版
     */
    @Override
    public Boolean updateByBo(ProcessTemplateBo bo) {
        ProcessTemplate update = BeanUtil.toBean(bo, ProcessTemplate.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ProcessTemplate entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除流程初始化模版
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
