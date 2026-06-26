package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAlgorithmModelExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.vo.AlgorithmModelVO;

import java.util.List;

/**
 * Algorithm model table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmModelService extends BaseService<AlgorithmModel> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmModel query parameters
	 * @return IPage<VlsAlgorithmModelVO>
	 */
	IPage<AlgorithmModelVO> selectVlsAlgorithmModelPage(IPage<AlgorithmModelVO> page, AlgorithmModelVO vlsAlgorithmModel);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmModelExcel>
	 */
	List<VlsAlgorithmModelExcel> exportVlsAlgorithmModel(Wrapper<AlgorithmModel> queryWrapper);

	/**
	 * Query algorithm model details by ID
	 *
	 * @param id model ID
	 * @return algorithm model
	 */
	AlgorithmModel getModelById(Long id);

	/**
	 * Create algorithm model
	 *
	 * @param createDTO creation parameters
	 * @return successfully created model
	 */
	AlgorithmModel createModel(AlgorithmModelVO createDTO);

	/**
	 * Delete algorithm model
	 *
	 * @param id model ID
	 * @return whether successful
	 */
	boolean deleteModel(Long id);

	/**
	 * Batch delete algorithm models
	 *
	 * @param ids model ID list
	 * @return whether successful
	 */
	boolean batchDeleteModel(List<Long> ids);

	/**
	 * Query model list by algorithm ID
	 *
	 * @param algorithmId algorithm ID
	 * @return model list
	 */
	List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId);

	/**
	 * Query model list by training task ID
	 *
	 * @param trainingId training task ID
	 * @return model list
	 */
	List<AlgorithmModel> getModelsByTrainingId(Long trainingId);

	/**
	 * Query model list by status
	 *
	 * @param status status
	 * @return model list
	 */
	List<AlgorithmModel> getModelsByStatus(String status);

	/**
	 * Publish model
	 *
	 * @param id model ID
	 * @return whether successful
	 */
	boolean publishModel(Long id);

	/**
	 * Unpublish model
	 *
	 * @param id model ID
	 * @return whether successful
	 */
	boolean unpublishModel(Long id);

	/**
	 * Batch publish models
	 *
	 * @param ids model ID list
	 * @return whether successful
	 */
	boolean batchPublishModel(List<Long> ids);

	/**
	 * Download model
	 *
	 * @param id model ID
	 * @return model file path
	 */
	String downloadModel(Long id);

	/**
	 * Deploy model
	 *
	 * @param id model ID
	 * @return whether successful
	 */
	boolean deployModel(Long id);

	/**
	 * Check if model name and version exist
	 *
	 * @param modelName model name
	 * @param version version
	 * @param excludeId excluded ID (used when updating)
	 * @return whether exists
	 */
	boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId);

	/**
	 * Query model by algorithm ID and version
	 *
	 * @param algorithmId algorithm ID
	 * @param version version
	 * @return algorithm model
	 */
	AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version);

	/**
	 * Get the latest version model under the algorithm
	 *
	 * @param algorithmId algorithm ID
	 * @return algorithm model
	 */
	AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId);

	/**
	 * Query Popular Models (Sorted by downloads)
	 *
	 * @param limit limit quantity
	 * @return model list
	 */
	List<AlgorithmModel> getPopularModels(Integer limit);

	/**
	 * Query model count by creator
	 *
	 * @param createdBy creator ID
	 * @return model quantity
	 */
	Long countModelsByCreatedBy(Long createdBy);

	/**
	 * Get total size of algorithm model
	 *
	 * @return total size (bytes)
	 */
	Long getTotalModelSize();

}
