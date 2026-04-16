package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.dto.AlgorithmTrainingStatisticsDTO;
import com.vlstream.entity.AlgorithmTraining;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Algorithm Training Task Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmTrainingMapper extends BaseMapper<AlgorithmTraining> {

    /**
     * Paginated query training tasks (including associated information)
     *
     * @param page Pagination object
     * @param taskName Task name
     * @param algorithmId Algorithm ID
     * @param datasetId Dataset ID
     * @param trainType Training type
     * @param trainStatus Training status
     * @param createdBy Creator
     * @param startTimeBegin Start time range start
     * @param startTimeEnd Start time range end
     * @param createdTimeBegin Creation time range start
     * @param createdTimeEnd Creation time range end
     * @param orderBy Order by field
     * @param order Order direction
     * @return Pagination result
     */
    IPage<AlgorithmTraining> selectPageWithDetails(
            Page<AlgorithmTraining> page,
            @Param("taskName") String taskName,
            @Param("algorithmId") Long algorithmId,
            @Param("datasetId") Long datasetId,
            @Param("trainType") String trainType,
            @Param("trainStatus") String trainStatus,
            @Param("createdBy") Long createdBy,
            @Param("startTimeBegin") LocalDateTime startTimeBegin,
            @Param("startTimeEnd") LocalDateTime startTimeEnd,
            @Param("createdTimeBegin") LocalDateTime createdTimeBegin,
            @Param("createdTimeEnd") LocalDateTime createdTimeEnd,
            @Param("orderBy") String orderBy,
            @Param("order") String order
    );

    /**
     * Query training task details by ID (including associated information)
     *
     * @param id Training task ID
     * @return Training task details
     */
    AlgorithmTraining selectByIdWithDetails(@Param("id") Long id);

    /**
     * Get training task statistics
     *
     * @return Statistics
     */
    AlgorithmTrainingStatisticsDTO selectStatistics();

    /**
     * Query training task list by algorithm ID
     *
     * @param algorithmId Algorithm ID
     * @return Training task list
     */
    List<AlgorithmTraining> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * Query training task list by dataset ID
     *
     * @param datasetId Dataset ID
     * @return Training task list
     */
    List<AlgorithmTraining> selectByDatasetId(@Param("datasetId") Long datasetId);

    /**
     * Get training tasks in progress
     *
     * @return Training tasks in progress
     */
    @Select("SELECT * FROM algorithm_training WHERE train_status = 'training' AND deleted = 0")
    List<AlgorithmTraining> selectTrainingTasks();

    /**
     * Get pending tasks
     *
     * @return Pending tasks
     */
    @Select("SELECT * FROM algorithm_training WHERE train_status = 'pending' AND deleted = 0 ORDER BY created_time ASC")
    List<AlgorithmTraining> selectPendingTasks();

    /**
     * Update training task status
     *
     * @param id Training task ID
     * @param trainStatus Training status
     * @param startTime Start time
     * @param endTime End time
     * @param errorMessage Error message
     * @return Updated rows
     */
    @Update("UPDATE algorithm_training SET train_status = #{trainStatus}, " +
            "start_time = #{startTime}, end_time = #{endTime}, error_message = #{errorMessage}, " +
            "updated_time = NOW() WHERE id = #{id}")
    int updateTrainStatus(@Param("id") Long id, 
                         @Param("trainStatus") String trainStatus,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime,
                         @Param("errorMessage") String errorMessage);

    /**
     * Update training progress
     *
     * @param id Training task ID
     * @param progress Progress percentage
     * @param epochCurrent Current epoch
     * @return Updated rows
     */
    @Update("UPDATE algorithm_training SET progress = #{progress}, " +
            "epoch_current = #{epochCurrent}, updated_time = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, 
                      @Param("progress") Integer progress,
                      @Param("epochCurrent") Integer epochCurrent);

    /**
     * Batch delete training tasks
     *
     * @param ids Task ID list
     * @return Deleted rows
     */
    @Update("<script>" +
            "UPDATE algorithm_training SET deleted = 1, updated_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Long> ids);
    
    /**
     * Query algorithm training task
     * 
     * @param id Algorithm training task primary key
     * @return Algorithm training task
     */
    AlgorithmTraining selectAlgorithmTrainingById(@Param("id") Long id);
    
    /**
     * Query algorithm training task list
     * 
     * @param algorithmTraining Algorithm training task
     * @return Algorithm training task collection
     */
    List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);
    
    /**
     * Add new algorithm training task
     * 
     * @param algorithmTraining Algorithm training task
     * @return Result
     */
    int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * Modify algorithm training task
     * 
     * @param algorithmTraining Algorithm training task
     * @return Result
     */
    int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * Delete algorithm training task
     * 
     * @param id Algorithm training task primary key
     * @return Result
     */
    int deleteAlgorithmTrainingById(@Param("id") Long id);
    
    /**
     * Batch delete algorithm training tasks
     * 
     * @param ids Primary key collection of algorithm training tasks to delete
     * @return Result
     */
    int deleteAlgorithmTrainingByIds(@Param("ids") Long[] ids);
} 