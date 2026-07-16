package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnalysisRequestExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnalysisRequestMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnalysisRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnalysisRequestVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnalysisRequestService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能分析请求表 服务实现类
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
