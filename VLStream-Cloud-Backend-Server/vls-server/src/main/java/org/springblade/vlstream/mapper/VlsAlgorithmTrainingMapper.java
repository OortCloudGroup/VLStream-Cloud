package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.vlstream.excel.VlsAlgorithmTrainingExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Algorithm training task table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmTrainingMapper extends BaseMapper<AlgorithmTraining> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmTraining query parameters
	 * @return List<VlsAlgorithmTrainingVO>
	 */
	List<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage page, AlgorithmTrainingVO vlsAlgorithmTraining);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmTrainingExcel>
	 */
	List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(@Param("ew") Wrapper<AlgorithmTraining> queryWrapper);

	/**
	 * Paginated query for training tasks (including association information)
	 *
	 * @param page pagination object
	 * @param taskName task name
	 * @param algorithmId algorithm ID
	 * @param datasetId dataset ID
	 * @param trainType training type
	 * @param trainStatus training status
	 * @param createdBy creator
	 * @param startTimeBegin start time range start
	 * @param startTimeEnd start time range end
	 * @param createdTimeBegin creation time range start
	 * @param createdTimeEnd creation time range end
	 * @param orderBy sort field
	 * @param order sort order
	 * @return pagination results
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
	 * Query training task details by ID (including association information)
	 *
	 * @param id training task ID
	 * @return training task details
	 */
	AlgorithmTraining selectByIdWithDetails(@Param("id") Long id);

	/**
	 * Query training task list by algorithm ID
	 *
	 * @param algorithmId algorithm ID
	 * @return training task list
	 */
	List<AlgorithmTraining> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * Query training task list by dataset ID
	 *
	 * @param datasetId dataset ID
	 * @return training task list
	 */
	List<AlgorithmTraining> selectByDatasetId(@Param("datasetId") Long datasetId);

	/**
	 * Get list of tasks currently training
	 *
	 * @return list of training tasks
	 */
	@Select("SELECT * FROM vls_algorithm_training WHERE train_status = 'training' AND is_deleted = 0")
	List<AlgorithmTraining> selectTrainingTasks();

	/**
	 * Get list of waiting tasks
	 *
	 * @return list of waiting tasks
	 */
	@Select("SELECT * FROM vls_algorithm_training WHERE train_status = 'pending' AND is_deleted = 0 ORDER BY create_time ASC")
	List<AlgorithmTraining> selectPendingTasks();

	/**
	 * Update training task status
	 *
	 * @param id training task ID
	 * @param trainStatus training status
	 * @param startTime start time
	 * @param endTime end time
	 * @param errorMessage error message
	 * @return number of updated rows
	 */
	@Update("UPDATE vls_algorithm_training SET train_status = #{trainStatus}, " +
		"start_time = #{startTime}, end_time = #{endTime}, error_message = #{errorMessage}, " +
		"update_time = NOW() WHERE id = #{id}")
	int updateTrainStatus(@Param("id") Long id,
						  @Param("trainStatus") String trainStatus,
						  @Param("startTime") LocalDateTime startTime,
						  @Param("endTime") LocalDateTime endTime,
						  @Param("errorMessage") String errorMessage);

	/**
	 * Update training progress
	 *
	 * @param id training task ID
	 * @param progress progress percentage
	 * @param epochCurrent current epoch
	 * @return number of updated rows
	 */
	@Update("UPDATE vls_algorithm_training SET progress = #{progress}, " +
		"epoch_current = #{epochCurrent}, update_time = NOW() WHERE id = #{id}")
	int updateProgress(@Param("id") Long id,
					   @Param("progress") Integer progress,
					   @Param("epochCurrent") Integer epochCurrent);

	/**
	 * Batch delete training tasks
	 *
	 * @param ids task ID list
	 * @return number of deleted rows
	 */
	@Update("<script>" +
		"UPDATE vls_algorithm_training SET deleted = 1, update_time = NOW() WHERE id IN " +
		"<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
		"#{id}" +
		"</foreach>" +
		"</script>")
	int deleteBatch(@Param("ids") List<Long> ids);

	/**
	 * Query Algorithm Training Tasks
	 *
	 * @param id algorithm training task primary key
	 * @return algorithm training task
	 */
	AlgorithmTraining selectAlgorithmTrainingById(@Param("id") Long id);

	/**
	 * Query Algorithm Training Task List
	 *
	 * @param algorithmTraining algorithm training task
	 * @return algorithm training task collection
	 */
	List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);

	/**
	 * Add algorithm training task
	 *
	 * @param algorithmTraining algorithm training task
	 * @return result
	 */
	int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Modify algorithm training task
	 *
	 * @param algorithmTraining algorithm training task
	 * @return result
	 */
	int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Delete algorithm training task
	 *
	 * @param id algorithm training task primary key
	 * @return result
	 */
	int deleteAlgorithmTrainingById(@Param("id") Long id);

	/**
	 * Batch delete algorithm training tasks
	 *
	 * @param ids set of primary keys of the algorithm training tasks to be deleted
	 * @return result
	 */
	int deleteAlgorithmTrainingByIds(@Param("ids") Long[] ids);

}
