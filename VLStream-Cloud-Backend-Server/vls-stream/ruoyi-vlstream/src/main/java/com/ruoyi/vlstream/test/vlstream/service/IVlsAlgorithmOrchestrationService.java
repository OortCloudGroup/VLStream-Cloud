/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.List;

/**
 * algorithm service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmOrchestrationService extends BaseService<AlgorithmOrchestration> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmOrchestration Query parameter
	 * @return IPage<VlsAlgorithmOrchestrationVO>
	 */
	IPage<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage<AlgorithmOrchestrationVO> page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmOrchestrationExcel>
	 */
	List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(Wrapper<AlgorithmOrchestration> queryWrapper);

}
