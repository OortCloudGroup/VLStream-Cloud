/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.rule.domain.RuleList;

import java.io.Serializable;
import java.util.Collection;

/**
 * @Description: 规则列表
 *
 * @Date:   2024-12-20
 * @Version: V1.0
 */
public interface IRuleListService extends IService<RuleList> {

	/**
	 * 添加一对多
	 *
	 * @param ruleList
	 */
	public void saveMain(RuleList ruleList) ;

	/**
	 * 修改一对多
	 *
   * @param ruleList
	 */
	public void updateMain(RuleList ruleList);

	/**
	 * 删除一对多
	 *
	 * @param id
	 */
	public void delMain (String id);

	/**
	 * 批量删除一对多
	 *
	 * @param idList
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);


    public int selectByTreeId(String TreeId);
}
