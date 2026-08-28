/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.List;

/**
 * algorithm Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmOrchestrationMapper extends BaseMapper<AlgorithmOrchestration> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmOrchestration Query parameter
	 * @return List<VlsAlgorithmOrchestrationVO>
	 */
	List<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmOrchestrationExcel>
	 */
	List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(@Param("ew") Wrapper<AlgorithmOrchestration> queryWrapper);

}
