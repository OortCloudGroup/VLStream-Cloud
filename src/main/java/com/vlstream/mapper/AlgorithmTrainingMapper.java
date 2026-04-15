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
 * 算法训练任务 Mapper 接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmTrainingMapper extends BaseMapper<AlgorithmTraining> {

    /**
     * 分页查询训练任务（包含关联信息）
     *
     * @param page 分页对象
     * @param taskName 任务名称
     * @param algorithmId 算法ID
     * @param datasetId 数据集ID
     * @param trainType 训练类型
     * @param trainStatus 训练状态
     * @param createdBy 创建人
     * @param startTimeBegin 开始时间范围开始
     * @param startTimeEnd 开始时间范围结束
     * @param createdTimeBegin 创建时间范围开始
     * @param createdTimeEnd 创建时间范围结束
     * @param orderBy 排序字段
     * @param order 排序方式
     * @return 分页结果
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
     * 根据ID查询训练任务详情（包含关联信息）
     *
     * @param id 训练任务ID
     * @return 训练任务详情
     */
    AlgorithmTraining selectByIdWithDetails(@Param("id") Long id);

    /**
     * 获取训练任务统计信息
     *
     * @return 统计信息
     */
    AlgorithmTrainingStatisticsDTO selectStatistics();

    /**
     * 根据算法ID查询训练任务列表
     *
     * @param algorithmId 算法ID
     * @return 训练任务列表
     */
    List<AlgorithmTraining> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * 根据数据集ID查询训练任务列表
     *
     * @param datasetId 数据集ID
     * @return 训练任务列表
     */
    List<AlgorithmTraining> selectByDatasetId(@Param("datasetId") Long datasetId);

    /**
     * 获取正在训练的任务列表
     *
     * @return 正在训练的任务列表
     */
    @Select("SELECT * FROM algorithm_training WHERE train_status = 'training' AND deleted = 0")
    List<AlgorithmTraining> selectTrainingTasks();

    /**
     * 获取等待中的任务列表
     *
     * @return 等待中的任务列表
     */
    @Select("SELECT * FROM algorithm_training WHERE train_status = 'pending' AND deleted = 0 ORDER BY created_time ASC")
    List<AlgorithmTraining> selectPendingTasks();

    /**
     * 更新训练任务状态
     *
     * @param id 训练任务ID
     * @param trainStatus 训练状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param errorMessage 错误信息
     * @return 更新行数
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
     * 更新训练进度
     *
     * @param id 训练任务ID
     * @param progress 进度百分比
     * @param epochCurrent 当前轮次
     * @return 更新行数
     */
    @Update("UPDATE algorithm_training SET progress = #{progress}, " +
            "epoch_current = #{epochCurrent}, updated_time = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, 
                      @Param("progress") Integer progress,
                      @Param("epochCurrent") Integer epochCurrent);

    /**
     * 批量删除训练任务
     *
     * @param ids 任务ID列表
     * @return 删除行数
     */
    @Update("<script>" +
            "UPDATE algorithm_training SET deleted = 1, updated_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Long> ids);
    
    /**
     * 查询算法训练任务
     * 
     * @param id 算法训练任务主键
     * @return 算法训练任务
     */
    AlgorithmTraining selectAlgorithmTrainingById(@Param("id") Long id);
    
    /**
     * 查询算法训练任务列表
     * 
     * @param algorithmTraining 算法训练任务
     * @return 算法训练任务集合
     */
    List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);
    
    /**
     * 新增算法训练任务
     * 
     * @param algorithmTraining 算法训练任务
     * @return 结果
     */
    int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * 修改算法训练任务
     * 
     * @param algorithmTraining 算法训练任务
     * @return 结果
     */
    int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * 删除算法训练任务
     * 
     * @param id 算法训练任务主键
     * @return 结果
     */
    int deleteAlgorithmTrainingById(@Param("id") Long id);
    
    /**
     * 批量删除算法训练任务
     * 
     * @param ids 需要删除的算法训练任务主键集合
     * @return 结果
     */
    int deleteAlgorithmTrainingByIds(@Param("ids") Long[] ids);
} 