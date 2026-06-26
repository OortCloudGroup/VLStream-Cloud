package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.vlstream.excel.VlsAlgorithmRepositoryExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;
import org.springblade.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.List;

/**
 * Algorithm repository table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmRepositoryMapper extends BaseMapper<AlgorithmRepository> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmRepository query parameters
	 * @return List<VlsAlgorithmRepositoryVO>
	 */
	List<AlgorithmRepositoryVO> selectVlsAlgorithmRepositoryPage(IPage page, AlgorithmRepositoryVO vlsAlgorithmRepository);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmRepositoryExcel>
	 */
	List<VlsAlgorithmRepositoryExcel> exportVlsAlgorithmRepository(@Param("ew") Wrapper<AlgorithmRepository> queryWrapper);

	/**
	 * Paginated query for algorithm repository list
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
	 * Query All Enabled Algorithm Repositories
	 */
	@Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND status = 'enabled' ORDER BY id")
	List<AlgorithmRepository> selectEnabledRepositories();

	/**
	 * Query algorithm repository by type
	 */
	@Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND repository_type = #{repositoryType} ORDER BY id")
	List<AlgorithmRepository> selectByRepositoryType(@Param("repositoryType") String repositoryType);

	/**
	 * Count algorithm repositories
	 */
	@Select("SELECT COUNT(*) FROM vls_algorithm_repository WHERE is_deleted = 0")
	Long countRepositories();

}
