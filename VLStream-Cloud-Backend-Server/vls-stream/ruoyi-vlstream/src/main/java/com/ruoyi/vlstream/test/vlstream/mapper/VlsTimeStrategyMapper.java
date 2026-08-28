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
import com.ruoyi.vlstream.test.vlstream.excel.VlsTimeStrategyExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;

import java.util.List;

/**
 * Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsTimeStrategyMapper extends BaseMapper<TimeStrategy> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsTimeStrategy Query parameter
	 * @return List<VlsTimeStrategyVO>
	 */
	List<TimeStrategyVO> selectVlsTimeStrategyPage(IPage page, TimeStrategyVO vlsTimeStrategy);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsTimeStrategyExcel>
	 */
	List<VlsTimeStrategyExcel> exportVlsTimeStrategy(@Param("ew") Wrapper<TimeStrategy> queryWrapper);

}
