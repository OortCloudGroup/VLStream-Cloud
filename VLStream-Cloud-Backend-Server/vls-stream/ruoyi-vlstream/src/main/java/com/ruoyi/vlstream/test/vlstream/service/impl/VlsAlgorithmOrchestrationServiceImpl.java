/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmOrchestrationMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmOrchestrationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 算法编排表 服务实现类
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
