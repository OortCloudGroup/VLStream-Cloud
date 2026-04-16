package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.AlgorithmModel;
import com.vlstream.dto.AlgorithmModelQueryDTO;
import com.vlstream.dto.AlgorithmModelStatisticsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Algorithm Model Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmModelMapper extends BaseMapper<AlgorithmModel> {

    /**
     * Paginated query algorithm model list
     *
     * @param page Pagination object
     * @param queryDTO Query parameters
     * @return Algorithm model list
     */
    IPage<AlgorithmModel> selectModelPage(Page<AlgorithmModel> page, @Param("query") AlgorithmModelQueryDTO queryDTO);

    /**
     * Query model list by algorithm ID
     *
     * @param algorithmId Algorithm ID
     * @return Algorithm model list
     */
    List<AlgorithmModel> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * Query model list by training task ID
     *
     * @param trainingId Training task ID
     * @return Algorithm model list
     */
    List<AlgorithmModel> selectByTrainingId(@Param("trainingId") Long trainingId);

    /**
     * Query model list by status
     *
     * @param status Status
     * @return Algorithm model list
     */
    List<AlgorithmModel> selectByStatus(@Param("status") String status);

    /**
     * Update model status
     *
     * @param id Model ID
     * @param status New status
     * @return Affected rows
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * Update model download count
     *
     * @param id Model ID
     * @return Affected rows
     */
    int updateDownloadCount(@Param("id") Long id);

    /**
     * Update model deployment count
     *
     * @param id Model ID
     * @return Affected rows
     */
    int updateDeployCount(@Param("id") Long id);

    /**
     * Batch update model status
     *
     * @param ids Model ID list
     * @param status New status
     * @return Affected rows
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * Get model statistics
     *
     * @return Statistics
     */
    AlgorithmModelStatisticsDTO getStatistics();

    /**
     * Check if model name and version exist
     *
     * @param modelName Model name
     * @param version Version
     * @param excludeId Exclude ID (used for updates)
     * @return Existence count
     */
    int checkModelNameAndVersion(@Param("modelName") String modelName, 
                                @Param("version") Integer version,
                                @Param("excludeId") Long excludeId);

    /**
     * Query model by algorithm ID and version
     *
     * @param algorithmId Algorithm ID
     * @param version Version
     * @return Algorithm model
     */
    AlgorithmModel selectByAlgorithmIdAndVersion(@Param("algorithmId") Long algorithmId, 
                                                @Param("version") Integer version);

    /**
     * Get the latest version model under an algorithm
     *
     * @param algorithmId Algorithm ID
     * @return Algorithm model
     */
    AlgorithmModel selectLatestByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * Query popular models (sorted by download count)
     *
     * @param limit Limit quantity
     * @return Algorithm model list
     */
    List<AlgorithmModel> selectPopularModels(@Param("limit") Integer limit);

    /**
     * Query model count by creator
     *
     * @param createdBy Creator ID
     * @return Model count
     */
    Long countByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * Get total algorithm model size (sum of file sizes of all published models)
     *
     * @return Total size (bytes)
     */
    Long getTotalModelSize();
} 