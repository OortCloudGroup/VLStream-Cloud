/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.rule.mapper;


import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.rule.domain.RuleTree;
import com.ruoyi.rule.vo.RuleTreeVo;

import java.util.List;

/**
 * Mapperinterface
 *
 * @author
 * @date 2024-12-17
 */
public interface RuleTreeMapper extends BaseMapperPlus<RuleTreeMapper, RuleTree, RuleTreeVo> {
    /**
     * Query
     *
     * @param id primary key
     * @return
     */
    public RuleTree selectRuleTreeById(String id);

    /**
     * Query list
     *
     * @param ruleTree
     * @return collection
     */
    public List<RuleTree> selectRuleTreeList(RuleTree ruleTree);


    /**
     * Update
     *
     * @param ruleTree
     * @return
     */
    public int updateRuleTree(RuleTree ruleTree);

    /**
     * Delete
     *
     * @param id primary key
     * @return
     */
    public int deleteRuleTreeById(String id);

    /**
     * Batch delete
     *
     * @param ids need to Delete dataprimary keycollection
     * @return
     */
    public int deleteRuleTreeByIds(String[] ids);
}
