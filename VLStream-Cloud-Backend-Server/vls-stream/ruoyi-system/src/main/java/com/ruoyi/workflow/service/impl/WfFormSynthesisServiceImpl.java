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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.WfFormSynthesis;
import com.ruoyi.workflow.domain.bo.WfFormSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfFormSynthesisVo;
import com.ruoyi.workflow.mapper.WfFormSynthesisMapper;
import com.ruoyi.workflow.service.IWfFormSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * form Service layer Process
 *
 * @author
 * @date 2024-12-25
 */
@RequiredArgsConstructor
@Service
public class WfFormSynthesisServiceImpl extends ServiceImpl<WfFormSynthesisMapper, WfFormSynthesis> implements IWfFormSynthesisService {

    private final WfFormSynthesisMapper baseMapper;

    /**
     * Query form
     */
    @Override
    public WfFormSynthesisVo queryById(String categoryId) {
        return baseMapper.selectVoById(categoryId);
    }


    /**
     * Query form list
     */
    @Override
    public Optional<List<WfFormSynthesisVo>> queryList(WfFormSynthesisBo bo) {
        LambdaQueryWrapper<WfFormSynthesis> lqw = buildQueryWrapper(bo);
        return Optional.ofNullable(baseMapper.selectVoList(lqw));
    }

    private LambdaQueryWrapper<WfFormSynthesis> buildQueryWrapper(WfFormSynthesisBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfFormSynthesis> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getUserId()), WfFormSynthesis::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), WfFormSynthesis::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), WfFormSynthesis::getCategoryName, bo.getCategoryName());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), WfFormSynthesis::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), WfFormSynthesis::getType, bo.getType());
        if (StringUtils.isBlank(bo.getCategoryId()) && StringUtils.isBlank(bo.getParentId())) {
            lqw.isNull(WfFormSynthesis::getParentId);  // if bo.getId() and bo.getParentId() is empty, Query parent_id to NULL
        }
        return lqw;
    }

    /**
     * Add form
     */
    @Override
    public Boolean insertByBo(WfFormSynthesisBo bo) {
        LambdaQueryWrapper<WfFormSynthesis> oldWfFormCategoryLambdaQueryWrapper = new LambdaQueryWrapper<WfFormSynthesis>();
        oldWfFormCategoryLambdaQueryWrapper.eq(WfFormSynthesis::getCategoryName, "系统审批");
        oldWfFormCategoryLambdaQueryWrapper.eq(WfFormSynthesis::getTenantId, bo.getTenantId());
        WfFormSynthesis oldOne = baseMapper.selectOne(oldWfFormCategoryLambdaQueryWrapper);
        if(oldOne!=null){
            throw new RuntimeException("系统审批分类已存在");
        }
        WfFormSynthesis add = BeanUtil.toBean(bo, WfFormSynthesis.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setCategoryId(add.getCategoryId());
        }
        return flag;
    }

    /**
     * Update form
     */
    @Override
    public Boolean updateByBo(WfFormSynthesisBo bo) {
        WfFormSynthesis update = BeanUtil.toBean(bo, WfFormSynthesis.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     */
    private void validEntityBeforeSave(WfFormSynthesis entity) {
        // TODO dataValidate ,
    }

    /**
     * Batch delete form
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO Validate ,Check whether need to Validate
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
