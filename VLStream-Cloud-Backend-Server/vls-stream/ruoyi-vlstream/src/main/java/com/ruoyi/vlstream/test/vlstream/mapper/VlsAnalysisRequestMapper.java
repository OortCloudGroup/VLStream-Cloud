/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnalysisRequestExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnalysisRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnalysisRequestVO;

import java.util.List;

/**
 * can Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnalysisRequestMapper extends BaseMapper<AnalysisRequest> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnalysisRequest Query parameter
	 * @return List<VlsAnalysisRequestVO>
	 */
	List<AnalysisRequestVO> selectVlsAnalysisRequestPage(IPage page, AnalysisRequestVO vlsAnalysisRequest);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnalysisRequestExcel>
	 */
	List<VlsAnalysisRequestExcel> exportVlsAnalysisRequest(@Param("ew") Wrapper<AnalysisRequest> queryWrapper);

}
