/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmModelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;

import java.util.List;

/**
 * algorithmmodel Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmModelMapper extends BaseMapper<AlgorithmModel> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmModel Query parameter
	 * @return List<VlsAlgorithmModelVO>
	 */
	List<AlgorithmModelVO> selectVlsAlgorithmModelPage(IPage page, AlgorithmModelVO vlsAlgorithmModel);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmModelExcel>
	 */
	List<VlsAlgorithmModelExcel> exportVlsAlgorithmModel(@Param("ew") Wrapper<AlgorithmModel> queryWrapper);

	/**
	 * algorithmIDQuery model list
	 *
	 * @param algorithmId algorithmID
	 * @return algorithmmodel
	 */
	List<AlgorithmModel> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * trainingtaskIDQuery model list
	 *
	 * @param trainingId trainingtaskID
	 * @return algorithmmodel
	 */
	List<AlgorithmModel> selectByTrainingId(@Param("trainingId") Long trainingId);

	/**
	 * Query model list
	 *
	 * @param status
	 * @return algorithmmodel
	 */
	List<AlgorithmModel> selectByStatus(@Param("status") String status);

	/**
	 * new model
	 *
	 * @param id modelID
	 * @param status new
	 * @return
	 */
	int updateStatus(@Param("id") Long id, @Param("status") String status);

	/**
	 * new model
	 *
	 * @param id modelID
	 * @return
	 */
	int updateDownloadCount(@Param("id") Long id);

	/**
	 * new model
	 *
	 * @param id modelID
	 * @return
	 */
	int updateDeployCount(@Param("id") Long id);

	/**
	 * new model
	 *
	 * @param ids modelID
	 * @param status new
	 * @return
	 */
	int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

	/**
	 * model and whether in
	 *
	 * @param modelName model
	 * @param version
	 * @param excludeId ID ( new )
	 * @return in
	 */
	int checkModelNameAndVersion(@Param("modelName") String modelName,
								 @Param("version") Integer version,
								 @Param("excludeId") Long excludeId);

	/**
	 * algorithmID and Query model
	 *
	 * @param algorithmId algorithmID
	 * @param version
	 * @return algorithmmodel
	 */
	AlgorithmModel selectByAlgorithmIdAndVersion(@Param("algorithmId") Long algorithmId,
												 @Param("version") Integer version);

	/**
	 * Get algorithm new model
	 *
	 * @param algorithmId algorithmID
	 * @return algorithmmodel
	 */
	AlgorithmModel selectLatestByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * Query model ( )
	 *
	 * @param limit
	 * @return algorithmmodel
	 */
	List<AlgorithmModel> selectPopularModels(@Param("limit") Integer limit);

	/**
	 * Query model
	 *
	 * @param createdBy ID
	 * @return model
	 */
	Long countByCreatedBy(@Param("createdBy") Long createdBy);

	/**
	 * Get algorithmmodel (all already model and )
	 *
	 * @return ( )
	 */
	Long getTotalModelSize();

}
