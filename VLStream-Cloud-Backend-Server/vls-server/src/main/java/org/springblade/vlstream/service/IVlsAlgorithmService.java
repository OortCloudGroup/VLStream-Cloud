package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAlgorithmExcel;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.vo.AlgorithmVO;

import java.util.List;
import java.util.Map;

/**
 * Algorithm table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmService extends BaseService<Algorithm> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithm query parameters
	 * @return IPage<VlsAlgorithmVO>
	 */
	IPage<AlgorithmVO> selectVlsAlgorithmPage(IPage<AlgorithmVO> page, AlgorithmVO vlsAlgorithm);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmExcel>
	 */
	List<VlsAlgorithmExcel> exportVlsAlgorithm(Wrapper<Algorithm> queryWrapper);

	/**
	 * Paginated query for algorithm list
	 *
	 * @param page pagination parameters
	 * @param repositoryId repository ID
	 * @param name algorithm name (fuzzy query)
	 * @param category algorithm type
	 * @param deployStatus deployment status
	 * @return pagination results
	 */
	IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page,
										 Long repositoryId,
										 String name,
										 String category,
										 String deployStatus);

	/**
	 * Query algorithm list by repository ID
	 *
	 * @param repositoryId repository ID
	 * @return algorithm list
	 */
	List<Algorithm> getByRepositoryId(Long repositoryId);

	/**
	 * Query algorithm list by category
	 *
	 * @param category algorithm category
	 * @return algorithm list
	 */
	List<Algorithm> getByCategory(String category);

	/**
	 * Create algorithm
	 *
	 * @param algorithm algorithm information
	 * @return whether successful
	 */
	boolean createAlgorithm(Algorithm algorithm);

	/**
	 * Update algorithm
	 *
	 * @param algorithm algorithm information
	 * @return whether successful
	 */
	boolean updateAlgorithm(Algorithm algorithm);

	/**
	 * Delete algorithm
	 *
	 * @param id algorithm ID
	 * @return whether successful
	 */
	boolean deleteAlgorithm(Long id);

	/**
	 * Batch delete algorithms
	 *
	 * @param ids algorithm ID list
	 * @return whether successful
	 */
	boolean batchDeleteAlgorithms(List<Long> ids);

	/**
	 * Update deployment status
	 *
	 * @param id algorithm ID
	 * @param deployStatus new deployment status
	 * @return whether successful
	 */
	boolean updateDeployStatus(Long id, String deployStatus);

	/**
	 * Batch update deployment status
	 *
	 * @param ids algorithm ID list
	 * @param deployStatus new deployment status
	 * @return whether successful
	 */
	boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus);

	/**
	 * Deploy algorithm to device
	 *
	 * @param algorithmId algorithm ID
	 * @param deviceIds device ID list
	 * @return whether successful
	 */
	boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds);

	/**
	 * Count algorithms under a repository
	 *
	 * @param repositoryId repository ID
	 * @return algorithm quantity
	 */
	Long countByRepositoryId(Long repositoryId);

	/**
	 * Get algorithm category statistics
	 *
	 * @return classification statistics
	 */
	List<Map<String, Object>> getCategoryStatistics();

	/**
	 * Get algorithm type statistics
	 *
	 * @return type statistics
	 */
	List<Map<String, Object>> getTypeStatistics();

	/**
	 * Get deployment status statistics
	 *
	 * @return Deployment status statistics
	 */
	List<Map<String, Object>> getDeployStatusStatistics();

	/**
	 * Algorithm evaluation
	 *
	 * @param algorithmId algorithm ID
	 * @return evaluation results
	 */
	Map<String, Object> evaluateAlgorithm(Long algorithmId);

}
