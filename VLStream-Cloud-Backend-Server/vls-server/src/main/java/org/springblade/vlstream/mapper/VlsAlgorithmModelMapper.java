package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.excel.VlsAlgorithmModelExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.vo.AlgorithmModelVO;

import java.util.List;

/**
 * Algorithm model table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmModelMapper extends BaseMapper<AlgorithmModel> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmModel query parameters
	 * @return List<VlsAlgorithmModelVO>
	 */
	List<AlgorithmModelVO> selectVlsAlgorithmModelPage(IPage page, AlgorithmModelVO vlsAlgorithmModel);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmModelExcel>
	 */
	List<VlsAlgorithmModelExcel> exportVlsAlgorithmModel(@Param("ew") Wrapper<AlgorithmModel> queryWrapper);

	/**
	 * Query model list by algorithm ID
	 *
	 * @param algorithmId algorithm ID
	 * @return algorithm model list
	 */
	List<AlgorithmModel> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * Query model list by training task ID
	 *
	 * @param trainingId training task ID
	 * @return algorithm model list
	 */
	List<AlgorithmModel> selectByTrainingId(@Param("trainingId") Long trainingId);

	/**
	 * Query model list by status
	 *
	 * @param status status
	 * @return algorithm model list
	 */
	List<AlgorithmModel> selectByStatus(@Param("status") String status);

	/**
	 * Update model status
	 *
	 * @param id model ID
	 * @param status new status
	 * @return affected rows
	 */
	int updateStatus(@Param("id") Long id, @Param("status") String status);

	/**
	 * Update model download count
	 *
	 * @param id model ID
	 * @return affected rows
	 */
	int updateDownloadCount(@Param("id") Long id);

	/**
	 * Update model deployment count
	 *
	 * @param id model ID
	 * @return affected rows
	 */
	int updateDeployCount(@Param("id") Long id);

	/**
	 * Batch update model status
	 *
	 * @param ids model ID list
	 * @param status new status
	 * @return affected rows
	 */
	int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

	/**
	 * Check if model name and version exist
	 *
	 * @param modelName model name
	 * @param version version
	 * @param excludeId excluded ID (used when updating)
	 * @return existing quantity
	 */
	int checkModelNameAndVersion(@Param("modelName") String modelName,
								 @Param("version") Integer version,
								 @Param("excludeId") Long excludeId);

	/**
	 * Query model by algorithm ID and version
	 *
	 * @param algorithmId algorithm ID
	 * @param version version
	 * @return algorithm model
	 */
	AlgorithmModel selectByAlgorithmIdAndVersion(@Param("algorithmId") Long algorithmId,
												 @Param("version") Integer version);

	/**
	 * Get the latest version model under the algorithm
	 *
	 * @param algorithmId algorithm ID
	 * @return algorithm model
	 */
	AlgorithmModel selectLatestByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * Query Popular Models (Sorted by downloads)
	 *
	 * @param limit limit quantity
	 * @return algorithm model list
	 */
	List<AlgorithmModel> selectPopularModels(@Param("limit") Integer limit);

	/**
	 * Query model count by creator
	 *
	 * @param createdBy creator ID
	 * @return model quantity
	 */
	Long countByCreatedBy(@Param("createdBy") Long createdBy);

	/**
	 * Get total size of algorithm model (sum of file sizes of all published models)
	 *
	 * @return total size (bytes)
	 */
	Long getTotalModelSize();

}
