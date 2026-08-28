/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnalysisRequestExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnalysisRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnalysisRequestVO;

import java.util.List;

/**
 * can service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnalysisRequestService extends BaseService<AnalysisRequest> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnalysisRequest Query parameter
	 * @return IPage<VlsAnalysisRequestVO>
	 */
	IPage<AnalysisRequestVO> selectVlsAnalysisRequestPage(IPage<AnalysisRequestVO> page, AnalysisRequestVO vlsAnalysisRequest);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnalysisRequestExcel>
	 */
	List<VlsAnalysisRequestExcel> exportVlsAnalysisRequest(Wrapper<AnalysisRequest> queryWrapper);

}
