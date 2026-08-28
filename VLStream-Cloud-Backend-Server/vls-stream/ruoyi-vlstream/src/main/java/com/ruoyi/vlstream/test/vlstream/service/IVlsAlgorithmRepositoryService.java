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
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmRepositoryExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.List;

/**
 * algorithm service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmRepositoryService extends BaseService<AlgorithmRepository> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmRepository Query parameter
	 * @return IPage<VlsAlgorithmRepositoryVO>
	 */
	IPage<AlgorithmRepositoryVO> selectVlsAlgorithmRepositoryPage(IPage<AlgorithmRepositoryVO> page, AlgorithmRepositoryVO vlsAlgorithmRepository);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmRepositoryExcel>
	 */
	List<VlsAlgorithmRepositoryExcel> exportVlsAlgorithmRepository(Wrapper<AlgorithmRepository> queryWrapper);

	/**
	 * Query algorithm list
	 *
	 * @param page parameter
	 * @param name ( Query )
	 * @param repositoryType
	 * @param status
	 * @return
	 */
	IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page,
													String name,
													String repositoryType,
													String status);

	/**
	 * Query all algorithm
	 *
	 * @return algorithm
	 */
	List<AlgorithmRepository> getEnabledRepositories();

	/**
	 * Query algorithm
	 *
	 * @param repositoryType
	 * @return algorithm
	 */
	List<AlgorithmRepository> getByRepositoryType(String repositoryType);

	/**
	 * algorithm
	 *
	 * @param repository algorithm info
	 * @return whether successfully
	 */
	boolean createRepository(AlgorithmRepository repository);

	/**
	 * new algorithm
	 *
	 * @param repository algorithm info
	 * @return whether successfully
	 */
	boolean updateRepository(AlgorithmRepository repository);

	/**
	 * Delete algorithm
	 *
	 * @param id ID
	 * @return whether successfully
	 */
	boolean deleteRepository(Long id);

	/**
	 * Batch delete algorithm
	 *
	 * @param ids ID
	 * @return whether successfully
	 */
	boolean batchDeleteRepositories(List<Long> ids);

	/**
	 * new
	 *
	 * @param id ID
	 * @param status new
	 * @return whether successfully
	 */
	boolean updateRepositoryStatus(Long id, String status);

	/**
	 * new
	 *
	 * @param ids ID
	 * @param status new
	 * @return whether successfully
	 */
	boolean batchUpdateRepositoryStatus(List<Long> ids, String status);

	/**
	 * algorithm
	 *
	 * @return
	 */
	Long countRepositories();

	/**
	 * new algorithm
	 *
	 * @param repositoryId ID
	 */
	void updateAlgorithmCount(Long repositoryId);

}
