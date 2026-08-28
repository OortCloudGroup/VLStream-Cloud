/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmVO;

import java.util.List;
import java.util.Map;

/**
 * algorithm service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmService extends BaseService<Algorithm> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithm Query parameter
	 * @return IPage<VlsAlgorithmVO>
	 */
	IPage<AlgorithmVO> selectVlsAlgorithmPage(IPage<AlgorithmVO> page, AlgorithmVO vlsAlgorithm);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmExcel>
	 */
	List<VlsAlgorithmExcel> exportVlsAlgorithm(Wrapper<Algorithm> queryWrapper);

	/**
	 * Query algorithm list
	 *
	 * @param page parameter
	 * @param repositoryId ID
	 * @param name algorithm ( Query )
	 * @param category algorithm
	 * @param deployStatus
	 * @return
	 */
	IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page,
										 Long repositoryId,
										 String name,
										 String category,
										 String deployStatus);

	/**
	 * IDQuery algorithm list
	 *
	 * @param repositoryId ID
	 * @return algorithm
	 */
	List<Algorithm> getByRepositoryId(Long repositoryId);

	/**
	 * Query algorithm list
	 *
	 * @param category algorithm
	 * @return algorithm
	 */
	List<Algorithm> getByCategory(String category);

	/**
	 * algorithm
	 *
	 * @param algorithm algorithminfo
	 * @return whether successfully
	 */
	boolean createAlgorithm(Algorithm algorithm);

	/**
	 * new algorithm
	 *
	 * @param algorithm algorithminfo
	 * @return whether successfully
	 */
	boolean updateAlgorithm(Algorithm algorithm);

	/**
	 * Delete algorithm
	 *
	 * @param id algorithmID
	 * @return whether successfully
	 */
	boolean deleteAlgorithm(Long id);

	/**
	 * Batch delete algorithm
	 *
	 * @param ids algorithmID
	 * @return whether successfully
	 */
	boolean batchDeleteAlgorithms(List<Long> ids);

	/**
	 * new
	 *
	 * @param id algorithmID
	 * @param deployStatus new
	 * @return whether successfully
	 */
	boolean updateDeployStatus(Long id, String deployStatus);

	/**
	 * new
	 *
	 * @param ids algorithmID
	 * @param deployStatus new
	 * @return whether successfully
	 */
	boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus);

	/**
	 * algorithm device
	 *
	 * @param algorithmId algorithmID
	 * @param deviceIds deviceID
	 * @return whether successfully
	 */
	boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds);

	/**
	 * algorithm
	 *
	 * @param repositoryId ID
	 * @return algorithm
	 */
	Long countByRepositoryId(Long repositoryId);

	/**
	 * Get algorithm
	 *
	 * @return info
	 */
	List<Map<String, Object>> getCategoryStatistics();

	/**
	 * Get algorithm
	 *
	 * @return info
	 */
	List<Map<String, Object>> getTypeStatistics();

	/**
	 * Get
	 *
	 * @return info
	 */
	List<Map<String, Object>> getDeployStatusStatistics();

	/**
	 * algorithm
	 *
	 * @param algorithmId algorithmID
	 * @return
	 */
	Map<String, Object> evaluateAlgorithm(Long algorithmId);

}
