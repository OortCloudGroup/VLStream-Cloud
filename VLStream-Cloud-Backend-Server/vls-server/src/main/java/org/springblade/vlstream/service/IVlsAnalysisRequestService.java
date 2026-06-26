package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAnalysisRequestExcel;
import org.springblade.vlstream.pojo.entity.AnalysisRequest;
import org.springblade.vlstream.pojo.vo.AnalysisRequestVO;

import java.util.List;

/**
 * Intelligent Analysis Request Table Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnalysisRequestService extends BaseService<AnalysisRequest> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnalysisRequest query parameters
	 * @return IPage<VlsAnalysisRequestVO>
	 */
	IPage<AnalysisRequestVO> selectVlsAnalysisRequestPage(IPage<AnalysisRequestVO> page, AnalysisRequestVO vlsAnalysisRequest);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnalysisRequestExcel>
	 */
	List<VlsAnalysisRequestExcel> exportVlsAnalysisRequest(Wrapper<AnalysisRequest> queryWrapper);

}
