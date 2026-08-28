/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmModelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;

import java.util.List;

/**
 * algorithmmodel service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmModelService extends BaseService<AlgorithmModel> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmModel Query parameter
	 * @return IPage<VlsAlgorithmModelVO>
	 */
	IPage<AlgorithmModelVO> selectVlsAlgorithmModelPage(IPage<AlgorithmModelVO> page, AlgorithmModelVO vlsAlgorithmModel);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmModelExcel>
	 */
	List<VlsAlgorithmModelExcel> exportVlsAlgorithmModel(Wrapper<AlgorithmModel> queryWrapper);

	/**
	 * IDQuery algorithmmodel
	 *
	 * @param id modelID
	 * @return algorithmmodel
	 */
	AlgorithmModel getModelById(Long id);

	/**
	 * algorithmmodel
	 *
	 * @param createDTO parameter
	 * @return successfully model
	 */
	AlgorithmModel createModel(AlgorithmModelVO createDTO);

	/**
	 * Delete algorithmmodel
	 *
	 * @param id modelID
	 * @return whether successfully
	 */
	boolean deleteModel(Long id);

	/**
	 * Batch delete algorithmmodel
	 *
	 * @param ids modelID
	 * @return whether successfully
	 */
	boolean batchDeleteModel(List<Long> ids);

	/**
	 * algorithmIDQuery model list
	 *
	 * @param algorithmId algorithmID
	 * @return model
	 */
	List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId);

	/**
	 * trainingtaskIDQuery model list
	 *
	 * @param trainingId trainingtaskID
	 * @return model
	 */
	List<AlgorithmModel> getModelsByTrainingId(Long trainingId);

	/**
	 * Query model list
	 *
	 * @param status
	 * @return model
	 */
	List<AlgorithmModel> getModelsByStatus(String status);

	/**
	 * model
	 *
	 * @param id modelID
	 * @return whether successfully
	 */
	boolean publishModel(Long id);

	/**
	 * model
	 *
	 * @param id modelID
	 * @return whether successfully
	 */
	boolean unpublishModel(Long id);

	/**
	 * model
	 *
	 * @param ids modelID
	 * @return whether successfully
	 */
	boolean batchPublishModel(List<Long> ids);

	/**
	 * model
	 *
	 * @param id modelID
	 * @return model
	 */
	String downloadModel(Long id);

	/**
	 * model
	 *
	 * @param id modelID
	 * @return whether successfully
	 */
	boolean deployModel(Long id);

	/**
	 * model and whether in
	 *
	 * @param modelName model
	 * @param version
	 * @param excludeId ID ( new )
	 * @return whether in
	 */
	boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId);

	/**
	 * algorithmID and Query model
	 *
	 * @param algorithmId algorithmID
	 * @param version
	 * @return algorithmmodel
	 */
	AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version);

	/**
	 * Get algorithm new model
	 *
	 * @param algorithmId algorithmID
	 * @return algorithmmodel
	 */
	AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId);

	/**
	 * Query model ( )
	 *
	 * @param limit
	 * @return model
	 */
	List<AlgorithmModel> getPopularModels(Integer limit);

	/**
	 * Query model
	 *
	 * @param createdBy ID
	 * @return model
	 */
	Long countModelsByCreatedBy(Long createdBy);

	/**
	 * Get algorithmmodel
	 *
	 * @return ( )
	 */
	Long getTotalModelSize();

}
