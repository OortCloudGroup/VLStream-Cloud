/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmTrainingExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.util.List;

/**
 * algorithmtrainingtask service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmTrainingService extends BaseService<AlgorithmTraining> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmTraining Query parameter
	 * @return IPage<VlsAlgorithmTrainingVO>
	 */
	IPage<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage<AlgorithmTrainingVO> page, AlgorithmTrainingVO vlsAlgorithmTraining);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmTrainingExcel>
	 */
	List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(Wrapper<AlgorithmTraining> queryWrapper);

	/**
	 * Query algorithmtrainingtask
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return algorithmtrainingtask
	 */
	public AlgorithmTraining selectAlgorithmTrainingById(Long id);

	/**
	 * Query algorithmtrainingtask list
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return algorithmtrainingtaskcollection
	 */
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);

	/**
	 * Add algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Update algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Batch delete algorithmtrainingtask
	 *
	 * @param ids need to Delete algorithmtrainingtaskprimary keycollection
	 * @return
	 */
	public int deleteAlgorithmTrainingByIds(Long[] ids);

	/**
	 * Delete algorithmtrainingtaskinfo
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return
	 */
	public int deleteAlgorithmTrainingById(Long id);

}
