/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.rule.domain.RuleList;
import com.ruoyi.rule.mapper.RuleListMapper;
import com.ruoyi.rule.service.IRuleListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;

/**
 * @Description:
 * @Date: 2024-12-20
 * @Version: V1.0
 */
@Service
public class RuleListServiceImpl extends ServiceImpl<RuleListMapper, RuleList> implements IRuleListService {

    @Autowired
    private RuleListMapper ruleListMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMain(RuleList ruleList) {
        ruleListMapper.insert(ruleList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMain(RuleList ruleList) {
        ruleListMapper.updateById(ruleList);

        // 1. Delete sub data
//		ruleConditionGroupMapper.deleteByMainId(ruleList.getId());

        // 2. sub data new
//		if(ruleConditionGroupList!=null && ruleConditionGroupList.size()>0) {
//			for(RuleConditionGroup entity:ruleConditionGroupList) {
// // Set
//				entity.setRuleListId(ruleList.getId());
//				ruleConditionGroupMapper.insert(entity);
//			}
//		}
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMain(String id) {
        ruleListMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            ruleListMapper.deleteById(id);
        }
    }

    @Override
    public int selectByTreeId(String TreeId) {
        LambdaQueryWrapper<RuleList > lambdaQueryWrapper =  new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(RuleList::getTreeId, TreeId);
        return ruleListMapper.delete(lambdaQueryWrapper);
    }

}
