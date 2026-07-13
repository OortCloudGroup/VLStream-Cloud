/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
 * @Description: 规则列表
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

        //1.先删除子表数据
//		ruleConditionGroupMapper.deleteByMainId(ruleList.getId());

        //2.子表数据重新插入
//		if(ruleConditionGroupList!=null && ruleConditionGroupList.size()>0) {
//			for(RuleConditionGroup entity:ruleConditionGroupList) {
//				//外键设置
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
