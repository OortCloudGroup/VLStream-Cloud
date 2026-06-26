package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsAnalysisRequestExcel;
import org.springblade.vlstream.mapper.VlsAnalysisRequestMapper;
import org.springblade.vlstream.pojo.entity.AnalysisRequest;
import org.springblade.vlstream.pojo.vo.AnalysisRequestVO;
import org.springblade.vlstream.service.IVlsAnalysisRequestService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Intelligent Analysis Request Table Service Implementation Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsAnalysisRequestServiceImpl extends BaseServiceImpl<VlsAnalysisRequestMapper, AnalysisRequest> implements IVlsAnalysisRequestService {

	@Override
	public IPage<AnalysisRequestVO> selectVlsAnalysisRequestPage(IPage<AnalysisRequestVO> page, AnalysisRequestVO vlsAnalysisRequest) {
		return page.setRecords(baseMapper.selectVlsAnalysisRequestPage(page, vlsAnalysisRequest));
	}

	@Override
	public List<VlsAnalysisRequestExcel> exportVlsAnalysisRequest(Wrapper<AnalysisRequest> queryWrapper) {
		List<VlsAnalysisRequestExcel> vlsAnalysisRequestList = baseMapper.exportVlsAnalysisRequest(queryWrapper);
		//vlsAnalysisRequestList.forEach(vlsAnalysisRequest -> {
		//	vlsAnalysisRequest.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAnalysisRequestEntity.getType()));
		//});
		return vlsAnalysisRequestList;
	}

}
