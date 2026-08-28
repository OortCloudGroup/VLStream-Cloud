/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsSceneGovernanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.SceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.SceneGovernanceVO;

import java.util.List;

/**
 * service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsSceneGovernanceService extends BaseService<SceneGovernance> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsSceneGovernance Query parameter
	 * @return IPage<VlsSceneGovernanceVO>
	 */
	IPage<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage<SceneGovernanceVO> page, SceneGovernanceVO vlsSceneGovernance);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsSceneGovernanceExcel>
	 */
	List<VlsSceneGovernanceExcel> exportVlsSceneGovernance(Wrapper<SceneGovernance> queryWrapper);

	/**
	 * Save scene governance and sync job.
	 *
	 * @param sceneGovernance Scene governance entity
	 * @return boolean
	 */
	boolean saveAndSchedule(SceneGovernance sceneGovernance);

	/**
	 * Update scene governance and sync job.
	 *
	 * @param sceneGovernance Scene governance entity
	 * @return boolean
	 */
	boolean updateAndSchedule(SceneGovernance sceneGovernance);

	/**
	 * Save or update scene governance and sync job.
	 *
	 * @param sceneGovernance Scene governance entity
	 * @return boolean
	 */
	boolean submitAndSchedule(SceneGovernance sceneGovernance);

}
