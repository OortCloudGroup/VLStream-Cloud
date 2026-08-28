/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import javax.annotation.Resource;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmTrainingExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * algorithmtrainingtask service
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsAlgorithmTrainingServiceImpl extends BaseServiceImpl<VlsAlgorithmTrainingMapper, AlgorithmTraining> implements IVlsAlgorithmTrainingService {

	@Resource
	private VlsAlgorithmTrainingMapper algorithmTrainingMapper;

	@Override
	public IPage<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage<AlgorithmTrainingVO> page, AlgorithmTrainingVO vlsAlgorithmTraining) {
		return page.setRecords(baseMapper.selectVlsAlgorithmTrainingPage(page, vlsAlgorithmTraining));
	}

	@Override
	public List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(Wrapper<AlgorithmTraining> queryWrapper) {
		List<VlsAlgorithmTrainingExcel> vlsAlgorithmTrainingList = baseMapper.exportVlsAlgorithmTraining(queryWrapper);
		//vlsAlgorithmTrainingList.forEach(vlsAlgorithmTraining -> {
		//	vlsAlgorithmTraining.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmTrainingEntity.getType()));
		//});
		return vlsAlgorithmTrainingList;
	}

	/**
	 * Query algorithmtrainingtask
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return algorithmtrainingtask
	 */
	@Override
	public AlgorithmTraining selectAlgorithmTrainingById(Long id) {
		return algorithmTrainingMapper.selectAlgorithmTrainingById(id);
	}

	/**
	 * Query algorithmtrainingtask list
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return algorithmtrainingtask
	 */
	@Override
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining) {
		return algorithmTrainingMapper.selectAlgorithmTrainingList(algorithmTraining);
	}

	/**
	 * Add algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	@Override
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return save(algorithmTraining) ? 1 : 0;
	}

	/**
	 * Update algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	@Override
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return updateById(algorithmTraining) ? 1 : 0;
	}

	/**
	 * Batch delete algorithmtrainingtask
	 *
	 * @param ids need to Delete algorithmtrainingtaskprimary key
	 * @return
	 */
	@Override
	public int deleteAlgorithmTrainingByIds(Long[] ids) {
		return algorithmTrainingMapper.deleteAlgorithmTrainingByIds(ids);
	}

	/**
	 * Delete algorithmtrainingtaskinfo
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return
	 */
	@Override
	public int deleteAlgorithmTrainingById(Long id) {
		return algorithmTrainingMapper.deleteAlgorithmTrainingById(id);
	}

}
