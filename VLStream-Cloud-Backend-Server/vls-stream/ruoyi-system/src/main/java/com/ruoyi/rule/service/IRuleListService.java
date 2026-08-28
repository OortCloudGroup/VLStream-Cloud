/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.rule.domain.RuleList;

import java.io.Serializable;
import java.util.Collection;

/**
 * @Description:
 *
 * @Date:   2024-12-20
 * @Version: V1.0
 */
public interface IRuleListService extends IService<RuleList> {

	/**
	 *
	 *
	 * @param ruleList
	 */
	public void saveMain(RuleList ruleList) ;

	/**
	 * Update
	 *
   * @param ruleList
	 */
	public void updateMain(RuleList ruleList);

	/**
	 * Delete
	 *
	 * @param id
	 */
	public void delMain (String id);

	/**
	 * Batch delete
	 *
	 * @param idList
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);


    public int selectByTreeId(String TreeId);
}
