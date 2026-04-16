package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AlgorithmModel;
import com.vlstream.dto.AlgorithmModelQueryDTO;
import com.vlstream.dto.AlgorithmModelCreateDTO;
import com.vlstream.dto.AlgorithmModelUpdateDTO;
import com.vlstream.dto.AlgorithmModelStatisticsDTO;

import java.util.List;

/**
 * Algorithm Model Service Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AlgorithmModelService extends IService<AlgorithmModel> {

    /**
     * Query algorithm models with pagination
     *
     * @param queryDTO Query parameters
     * @return Pagination result
     */
    IPage<AlgorithmModel> getModelPage(AlgorithmModelQueryDTO queryDTO);

    /**
     * Query algorithm model details by ID
     *
     * @param id Model ID
     * @return Algorithm model
     */
    AlgorithmModel getModelById(Long id);

    /**
     * Create algorithm model
     *
     * @param createDTO Create parameters
     * @return Created model
     */
    AlgorithmModel createModel(AlgorithmModelCreateDTO createDTO);

    /**
     * Update algorithm model
     *
     * @param updateDTO Update parameters
     * @return Updated model
     */
    AlgorithmModel updateModel(AlgorithmModelUpdateDTO updateDTO);

    /**
     * Delete algorithm model
     *
     * @param id Model ID
     * @return Whether successful
     */
    boolean deleteModel(Long id);

    /**
     * Batch delete algorithm models
     *
     * @param ids Model ID list
     * @return Whether successful
     */
    boolean batchDeleteModel(List<Long> ids);

    /**
     * Query model list by algorithm ID
     *
     * @param algorithmId Algorithm ID
     * @return Model list
     */
    List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId);

    /**
     * Query model list by training task ID
     *
     * @param trainingId Training task ID
     * @return Model list
     */
    List<AlgorithmModel> getModelsByTrainingId(Long trainingId);

    /**
     * Query model list by status
     *
     * @param status Status
     * @return Model list
     */
    List<AlgorithmModel> getModelsByStatus(String status);

    /**
     * Publish model
     *
     * @param id Model ID
     * @return Whether successful
     */
    boolean publishModel(Long id);

    /**
     * Unpublish model
     *
     * @param id Model ID
     * @return Whether successful
     */
    boolean unpublishModel(Long id);

    /**
     * Batch publish models
     *
     * @param ids Model ID list
     * @return Whether successful
     */
    boolean batchPublishModel(List<Long> ids);

    /**
     * Download model
     *
     * @param id Model ID
     * @return Model file path
     */
    String downloadModel(Long id);

    /**
     * Deploy model
     *
     * @param id Model ID
     * @return Whether successful
     */
    boolean deployModel(Long id);

    /**
     * Get model statistics
     *
     * @return Statistics information
     */
    AlgorithmModelStatisticsDTO getStatistics();

    /**
     * Check if model name and version exist
     *
     * @param modelName Model name
     * @param version Version
     * @param excludeId Excluded ID (used for update)
     * @return Whether exists
     */
    boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId);

    /**
     * Query model by algorithm ID and version
     *
     * @param algorithmId Algorithm ID
     * @param version Version
     * @return Algorithm model
     */
    AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version);

    /**
     * Get latest version model by algorithm ID
     *
     * @param algorithmId Algorithm ID
     * @return Algorithm model
     */
    AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId);

    /**
     * Query popular models (sorted by download count)
     *
     * @param limit Limit count
     * @return Model list
     */
    List<AlgorithmModel> getPopularModels(Integer limit);

    /**
     * Query model count by creator
     *
     * @param createdBy Creator ID
     * @return Model count
     */
    Long countModelsByCreatedBy(Long createdBy);

    /**
     * Get total size of algorithm models
     *
     * @return Total size (bytes)
     */
    Long getTotalModelSize();
} 