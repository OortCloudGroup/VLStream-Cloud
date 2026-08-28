/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmVO;

import java.util.List;
import java.util.Map;

/**
 * algorithm Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmMapper extends BaseMapper<Algorithm> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithm Query parameter
	 * @return List<VlsAlgorithmVO>
	 */
	List<AlgorithmVO> selectVlsAlgorithmPage(IPage page, @Param("vlsAlgorithm") AlgorithmVO vlsAlgorithm);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmExcel>
	 */
	List<VlsAlgorithmExcel> exportVlsAlgorithm(@Param("ew") Wrapper<Algorithm> queryWrapper);

	/**
	 * Query algorithm list
	 */
	@Select("SELECT a.*, r.name as repository_name " +
		"FROM vls_algorithm a " +
		"LEFT JOIN vls_algorithm_repository r ON a.repository_id = r.id " +
		"WHERE a.is_deleted = 0 " +
		"AND (#{repositoryId} IS NULL OR a.repository_id = #{repositoryId}) " +
		"AND (#{name} IS NULL OR a.name LIKE CONCAT('%', #{name}, '%')) " +
		"AND (#{category} IS NULL OR a.category = #{category}) " +
		"AND (#{deployStatus} IS NULL OR a.deploy_status = #{deployStatus}) ")
	IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page,
										 @Param("repositoryId") Long repositoryId,
										 @Param("name") String name,
										 @Param("category") String category,
										 @Param("deployStatus") String deployStatus);

	/**
	 * IDQuery algorithm list
	 */
	@Select("SELECT * FROM vls_algorithm WHERE is_deleted = 0 AND repository_id = #{repositoryId} ORDER BY create_time DESC")
	List<Algorithm> selectByRepositoryId(@Param("repositoryId") Long repositoryId);

	/**
	 * Query algorithm list
	 */
	@Select("SELECT * FROM vls_algorithm WHERE is_deleted = 0 AND category = #{category} ORDER BY create_time DESC")
	List<Algorithm> selectByCategory(@Param("category") String category);

	/**
	 * algorithm
	 */
	@Select("SELECT COUNT(*) FROM vls_algorithm WHERE is_deleted = 0 AND repository_id = #{repositoryId}")
	Long countByRepositoryId(@Param("repositoryId") Long repositoryId);

	/**
	 * Query algorithm
	 */
	@Select("SELECT category, COUNT(*) as count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY category")
	List<Map<String, Object>> selectCategoryStatistics();

	/**
	 * Query algorithm
	 */
	@Select("SELECT type, COUNT(*) as count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY type")
	List<Map<String, Object>> selectTypeStatistics();

	/**
	 * Query
	 */
	@Select("SELECT deploy_status, COUNT(*) as count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY deploy_status")
	List<Map<String, Object>> selectDeployStatusStatistics();

}
