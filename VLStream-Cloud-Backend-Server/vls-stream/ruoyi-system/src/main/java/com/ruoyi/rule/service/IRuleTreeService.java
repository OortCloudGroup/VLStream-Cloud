/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.rule.service;


import com.ruoyi.rule.bo.RuleTreeBo;
import com.ruoyi.rule.vo.RuleTreeVo;

import java.util.Collection;
import java.util.List;

/**
 * Serviceinterface
 *
 * @author
 * @date 2024-12-17
 */
public interface IRuleTreeService
{
    /**
     * Query
     */
    RuleTreeVo queryById(String id);


    /**
     * Query list
     */
    List<RuleTreeVo> queryList(RuleTreeBo bo);

    /**
     * Add
     */
    Boolean insertByBo(RuleTreeBo bo);

    /**
     * Update
     */
    Boolean updateByBo(RuleTreeBo bo);

    /**
     * Validate Batch delete info
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
