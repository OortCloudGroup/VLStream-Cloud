/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmRepositoryExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.List;

/**
 * algorithm Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmRepositoryMapper extends BaseMapper<AlgorithmRepository> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT r.id, r.name, 0 AS parent_id, r.sort_order, r.status, r.remark, r.repository_type, "
        + "r.shared_catalog_id, 1 AS catalog_read_only, "
        + "(SELECT COUNT(*) FROM vls_algorithm a JOIN vls_algorithm_repository source ON source.id = a.repository_id "
        + "WHERE source.shared_catalog_id = r.id AND source.is_deleted = 0 AND a.is_deleted = 0 "
        + "AND (source.id = r.id OR (a.tenant_id = source.tenant_id AND a.catalog_seed = 0))) AS algorithm_count "
        + "FROM vls_algorithm_repository r WHERE r.id = r.shared_catalog_id AND r.is_deleted = 0")
    List<AlgorithmRepository> selectSharedCatalogs();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT shared_catalog_id FROM vls_algorithm_repository "
        + "WHERE id = #{id} AND id = shared_catalog_id AND status = 1 AND is_deleted = 0")
    Long selectAssignableSharedCatalog(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT shared_catalog_id FROM vls_algorithm_repository "
        + "WHERE id = #{id} AND id = shared_catalog_id AND status = 1 AND is_deleted = 0 FOR UPDATE")
    Long lockAssignableSharedCatalog(@Param("id") Long id);

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmRepository Query parameter
	 * @return List<VlsAlgorithmRepositoryVO>
	 */
	List<AlgorithmRepositoryVO> selectVlsAlgorithmRepositoryPage(IPage page, AlgorithmRepositoryVO vlsAlgorithmRepository);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmRepositoryExcel>
	 */
	List<VlsAlgorithmRepositoryExcel> exportVlsAlgorithmRepository(@Param("ew") Wrapper<AlgorithmRepository> queryWrapper);

	/**
	 * Query algorithm list
	 */
	@Select("SELECT r.*, " +
		"(SELECT COUNT(*) FROM algorithm a WHERE a.repository_id = r.id AND a.is_deleted = 0) as algorithm_count " +
		"FROM vls_algorithm_repository r " +
		"WHERE r.is_deleted = 0 " +
		"AND (#{name} IS NULL OR r.name LIKE CONCAT('%', #{name}, '%')) " +
		"AND (#{repositoryType} IS NULL OR r.repository_type = #{repositoryType}) " +
		"AND (#{status} IS NULL OR r.status = #{status}) " +
		"ORDER BY r.id ASC")
	IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page,
													@Param("name") String name,
													@Param("repositoryType") String repositoryType,
													@Param("status") String status);

	/**
	 * Query all algorithm
	 */
	@Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND status = 'enabled' ORDER BY id")
	List<AlgorithmRepository> selectEnabledRepositories();

	/**
	 * Query algorithm
	 */
	@Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND repository_type = #{repositoryType} ORDER BY id")
	List<AlgorithmRepository> selectByRepositoryType(@Param("repositoryType") String repositoryType);

	/**
	 * algorithm
	 */
	@Select("SELECT COUNT(*) FROM vls_algorithm_repository WHERE is_deleted = 0")
	Long countRepositories();

}
