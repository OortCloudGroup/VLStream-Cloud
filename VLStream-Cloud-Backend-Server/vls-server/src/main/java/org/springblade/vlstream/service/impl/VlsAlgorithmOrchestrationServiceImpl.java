package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmOrchestrationMapper;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;
import org.springblade.vlstream.pojo.vo.AlgorithmOrchestrationVO;
import org.springblade.vlstream.service.IVlsAlgorithmOrchestrationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Algorithm orchestration table service implementation class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsAlgorithmOrchestrationServiceImpl extends BaseServiceImpl<VlsAlgorithmOrchestrationMapper, AlgorithmOrchestration> implements IVlsAlgorithmOrchestrationService {

	@Override
	public IPage<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage<AlgorithmOrchestrationVO> page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration) {
		return page.setRecords(baseMapper.selectVlsAlgorithmOrchestrationPage(page, vlsAlgorithmOrchestration));
	}

	@Override
	public List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(Wrapper<AlgorithmOrchestration> queryWrapper) {
		List<VlsAlgorithmOrchestrationExcel> vlsAlgorithmOrchestrationList = baseMapper.exportVlsAlgorithmOrchestration(queryWrapper);
		//vlsAlgorithmOrchestrationList.forEach(vlsAlgorithmOrchestration -> {
		//	vlsAlgorithmOrchestration.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmOrchestrationEntity.getType()));
		//});
		return vlsAlgorithmOrchestrationList;
	}

}
