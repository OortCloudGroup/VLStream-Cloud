package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAlgorithmRepositoryExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;
import org.springblade.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.List;

/**
 * Algorithm repository table Service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmRepositoryService extends BaseService<AlgorithmRepository> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmRepository query parameters
	 * @return IPage<VlsAlgorithmRepositoryVO>
	 */
	IPage<AlgorithmRepositoryVO> selectVlsAlgorithmRepositoryPage(IPage<AlgorithmRepositoryVO> page, AlgorithmRepositoryVO vlsAlgorithmRepository);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmRepositoryExcel>
	 */
	List<VlsAlgorithmRepositoryExcel> exportVlsAlgorithmRepository(Wrapper<AlgorithmRepository> queryWrapper);

	/**
	 * Paginated query for algorithm repository list
	 *
	 * @param page pagination parameters
	 * @param name repository name (fuzzy query)
	 * @param repositoryType repository type
	 * @param status status
	 * @return pagination results
	 */
	IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page,
													String name,
													String repositoryType,
													String status);

	/**
	 * Query All Enabled Algorithm Repositories
	 *
	 * @return list of enabled algorithm repositories
	 */
	List<AlgorithmRepository> getEnabledRepositories();

	/**
	 * Query algorithm repository by type
	 *
	 * @param repositoryType repository type
	 * @return algorithm repository list
	 */
	List<AlgorithmRepository> getByRepositoryType(String repositoryType);

	/**
	 * Create algorithm repository
	 *
	 * @param repository algorithm repository information
	 * @return whether successful
	 */
	boolean createRepository(AlgorithmRepository repository);

	/**
	 * Update algorithm repository
	 *
	 * @param repository algorithm repository information
	 * @return whether successful
	 */
	boolean updateRepository(AlgorithmRepository repository);

	/**
	 * Delete algorithm repository
	 *
	 * @param id repository ID
	 * @return whether successful
	 */
	boolean deleteRepository(Long id);

	/**
	 * Batch delete algorithm repositories
	 *
	 * @param ids repository ID list
	 * @return whether successful
	 */
	boolean batchDeleteRepositories(List<Long> ids);

	/**
	 * Update repository status
	 *
	 * @param id repository ID
	 * @param status new status
	 * @return whether successful
	 */
	boolean updateRepositoryStatus(Long id, String status);

	/**
	 * Batch update repository status
	 *
	 * @param ids repository ID list
	 * @param status new status
	 * @return whether successful
	 */
	boolean batchUpdateRepositoryStatus(List<Long> ids, String status);

	/**
	 * Count algorithm repositories
	 *
	 * @return total number of repositories
	 */
	Long countRepositories();

	/**
	 * Update algorithm count of repository
	 *
	 * @param repositoryId repository ID
	 */
	void updateAlgorithmCount(Long repositoryId);

}
