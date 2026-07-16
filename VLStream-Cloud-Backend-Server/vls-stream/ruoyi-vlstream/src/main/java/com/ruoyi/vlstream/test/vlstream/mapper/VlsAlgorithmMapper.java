/*
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
 * 算法表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmMapper extends BaseMapper<Algorithm> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithm 查询参数
	 * @return List<VlsAlgorithmVO>
	 */
	List<AlgorithmVO> selectVlsAlgorithmPage(IPage page, AlgorithmVO vlsAlgorithm);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmExcel>
	 */
	List<VlsAlgorithmExcel> exportVlsAlgorithm(@Param("ew") Wrapper<Algorithm> queryWrapper);

	/**
	 * 分页查询算法列表
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
	 * 根据仓库ID查询算法列表
	 */
	@Select("SELECT * FROM vls_algorithm WHERE is_deleted = 0 AND repository_id = #{repositoryId} ORDER BY create_time DESC")
	List<Algorithm> selectByRepositoryId(@Param("repositoryId") Long repositoryId);

	/**
	 * 根据分类查询算法列表
	 */
	@Select("SELECT * FROM vls_algorithm WHERE is_deleted = 0 AND category = #{category} ORDER BY create_time DESC")
	List<Algorithm> selectByCategory(@Param("category") String category);

	/**
	 * 统计某仓库下的算法数量
	 */
	@Select("SELECT COUNT(*) FROM vls_algorithm WHERE is_deleted = 0 AND repository_id = #{repositoryId}")
	Long countByRepositoryId(@Param("repositoryId") Long repositoryId);

	/**
	 * 查询算法分类统计
	 */
	@Select("SELECT category, COUNT(*) as count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY category")
	List<Map<String, Object>> selectCategoryStatistics();

	/**
	 * 查询算法类型统计
	 */
	@Select("SELECT type, COUNT(*) as count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY type")
	List<Map<String, Object>> selectTypeStatistics();

	/**
	 * 查询部署状态统计
	 */
	@Select("SELECT deploy_status, COUNT(*) as count FROM vls_algorithm WHERE is_deleted = 0 GROUP BY deploy_status")
	List<Map<String, Object>> selectDeployStatusStatistics();

}
