/*
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
 * 算法训练任务表 服务实现类
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
	 * 查询算法训练任务
	 *
	 * @param id 算法训练任务主键
	 * @return 算法训练任务
	 */
	@Override
	public AlgorithmTraining selectAlgorithmTrainingById(Long id) {
		return algorithmTrainingMapper.selectAlgorithmTrainingById(id);
	}

	/**
	 * 查询算法训练任务列表
	 *
	 * @param algorithmTraining 算法训练任务
	 * @return 算法训练任务
	 */
	@Override
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining) {
		return algorithmTrainingMapper.selectAlgorithmTrainingList(algorithmTraining);
	}

	/**
	 * 新增算法训练任务
	 *
	 * @param algorithmTraining 算法训练任务
	 * @return 结果
	 */
	@Override
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return save(algorithmTraining) ? 1 : 0;
	}

	/**
	 * 修改算法训练任务
	 *
	 * @param algorithmTraining 算法训练任务
	 * @return 结果
	 */
	@Override
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return updateById(algorithmTraining) ? 1 : 0;
	}

	/**
	 * 批量删除算法训练任务
	 *
	 * @param ids 需要删除的算法训练任务主键
	 * @return 结果
	 */
	@Override
	public int deleteAlgorithmTrainingByIds(Long[] ids) {
		return algorithmTrainingMapper.deleteAlgorithmTrainingByIds(ids);
	}

	/**
	 * 删除算法训练任务信息
	 *
	 * @param id 算法训练任务主键
	 * @return 结果
	 */
	@Override
	public int deleteAlgorithmTrainingById(Long id) {
		return algorithmTrainingMapper.deleteAlgorithmTrainingById(id);
	}

}
