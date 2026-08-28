/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.MobileSceneGovernanceLoopVO;

/**
 * main task service
 */
public interface IVlsMobileSceneGovernanceService extends BaseService<MobileSceneGovernance> {

	/**
	 * Add
	 */
	boolean saveImmediate(MobileSceneGovernance mobileSceneGovernance);

	/**
	 * Add loop , Generate sub looptask
	 */
	boolean saveLoop(MobileSceneGovernance mobileSceneGovernance);

	/**
	 * Query list
	 */
	IPage<MobileSceneGovernance> listImmediate(IPage<MobileSceneGovernance> page);

	/**
	 * Query loop list ( sub looptask)
	 */
	IPage<MobileSceneGovernanceLoopVO> listLoop(IPage<MobileSceneGovernance> page);
}
