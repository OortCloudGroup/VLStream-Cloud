/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.rule.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.rule.bo.RuleTreeBo;
import com.ruoyi.rule.domain.RuleTree;
import com.ruoyi.rule.mapper.RuleTreeMapper;
import com.ruoyi.rule.service.IRuleTreeService;
import com.ruoyi.rule.vo.RuleTreeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service layer Process
 *
 * @author
 * @date 2024-12-18
 */
@RequiredArgsConstructor
@Service
public class RuleTreeServiceImpl implements IRuleTreeService {

    private final RuleTreeMapper baseMapper;

    /**
     * Query
     */
    @Override
    public RuleTreeVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }


    /**
     * Query list
     */
    @Override
    public List<RuleTreeVo> queryList(RuleTreeBo bo) {
        LambdaQueryWrapper<RuleTree> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<RuleTree> buildQueryWrapper(RuleTreeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<RuleTree> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getUserId()), RuleTree::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), RuleTree::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), RuleTree::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), RuleTree::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), RuleTree::getType, bo.getType());
        if (StringUtils.isBlank(bo.getId()) && StringUtils.isBlank(bo.getParentId())) {
            lqw.isNull(RuleTree::getParentId);  // if bo.getId() and bo.getParentId() is empty, Query parent_id to NULL
        }
        return lqw;
    }

    /**
     * Add
     */
    @Override
    public Boolean insertByBo(RuleTreeBo bo) {
        RuleTree add = BeanUtil.toBean(bo, RuleTree.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        return flag;
    }

    /**
     * Update
     */
    @Override
    public Boolean updateByBo(RuleTreeBo bo) {
        RuleTree update = BeanUtil.toBean(bo, RuleTree.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     */
    private void validEntityBeforeSave(RuleTree entity) {
        // TODO dataValidate ,
    }

    /**
     * Batch delete
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO Validate ,Check whether need to Validate
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
