/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmTrainingExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * algorithmtrainingtask Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmTrainingMapper extends BaseMapper<AlgorithmTraining> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmTraining Query parameter
	 * @return List<VlsAlgorithmTrainingVO>
	 */
	List<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage page, AlgorithmTrainingVO vlsAlgorithmTraining);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmTrainingExcel>
	 */
	List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(@Param("ew") Wrapper<AlgorithmTraining> queryWrapper);

	/**
	 * Query trainingtask ( info)
	 *
	 * @param page object
	 * @param taskName task
	 * @param algorithmId algorithmID
	 * @param datasetId datasetID
	 * @param trainType training
	 * @param trainStatus training
	 * @param createdBy
	 * @param startTimeBegin start start
	 * @param startTimeEnd start finish
	 * @param createdTimeBegin create time start
	 * @param createdTimeEnd create time finish
	 * @param orderBy field
	 * @param order
	 * @return
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
	 * IDQuery trainingtask ( info)
	 *
	 * @param id trainingtaskID
	 * @return trainingtask
	 */
	AlgorithmTraining selectByIdWithDetails(@Param("id") Long id);

	/**
	 * algorithmIDQuery trainingtask list
	 *
	 * @param algorithmId algorithmID
	 * @return trainingtask
	 */
	List<AlgorithmTraining> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * datasetIDQuery trainingtask list
	 *
	 * @param datasetId datasetID
	 * @return trainingtask
	 */
	List<AlgorithmTraining> selectByDatasetId(@Param("datasetId") Long datasetId);

	/**
	 * Get in training task
	 *
	 * @return in training task
	 */
	@Select("SELECT * FROM vls_algorithm_training WHERE train_status = 'training' AND is_deleted = 0")
	List<AlgorithmTraining> selectTrainingTasks();

	/**
	 * Get etc. in task
	 *
	 * @return etc. in task
	 */
	@Select("SELECT * FROM vls_algorithm_training WHERE train_status = 'pending' AND is_deleted = 0 ORDER BY create_time ASC")
	List<AlgorithmTraining> selectPendingTasks();

	/**
	 * new trainingtask
	 *
	 * @param id trainingtaskID
	 * @param trainStatus training
	 * @param startTime start
	 * @param endTime finish
	 * @param errorMessage info
	 * @return new
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
	 * new training
	 *
	 * @param id trainingtaskID
	 * @param progress
	 * @param epochCurrent current
	 * @return new
	 */
	@Update("UPDATE vls_algorithm_training SET progress = #{progress}, " +
		"epoch_current = #{epochCurrent}, update_time = NOW() WHERE id = #{id}")
	int updateProgress(@Param("id") Long id,
					   @Param("progress") Integer progress,
					   @Param("epochCurrent") Integer epochCurrent);

	/**
	 * Batch delete trainingtask
	 *
	 * @param ids taskID
	 * @return Delete
	 */
	@Update("<script>" +
		"UPDATE vls_algorithm_training SET deleted = 1, update_time = NOW() WHERE id IN " +
		"<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
		"#{id}" +
		"</foreach>" +
		"</script>")
	int deleteBatch(@Param("ids") List<Long> ids);

	/**
	 * Query algorithmtrainingtask
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return algorithmtrainingtask
	 */
	AlgorithmTraining selectAlgorithmTrainingById(@Param("id") Long id);

	/**
	 * Query algorithmtrainingtask list
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return algorithmtrainingtaskcollection
	 */
	List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);

	/**
	 * Add algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Update algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Delete algorithmtrainingtask
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return
	 */
	int deleteAlgorithmTrainingById(@Param("id") Long id);

	/**
	 * Batch delete algorithmtrainingtask
	 *
	 * @param ids need to Delete algorithmtrainingtaskprimary keycollection
	 * @return
	 */
	int deleteAlgorithmTrainingByIds(@Param("ids") Long[] ids);

}
