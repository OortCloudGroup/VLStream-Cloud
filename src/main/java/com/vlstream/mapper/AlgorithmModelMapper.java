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
 * 算法模型Mapper接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AlgorithmModelMapper extends BaseMapper<AlgorithmModel> {

    /**
     * 分页查询算法模型列表
     *
     * @param page 分页对象
     * @param queryDTO 查询参数
     * @return 算法模型列表
     */
    IPage<AlgorithmModel> selectModelPage(Page<AlgorithmModel> page, @Param("query") AlgorithmModelQueryDTO queryDTO);

    /**
     * 根据算法ID查询模型列表
     *
     * @param algorithmId 算法ID
     * @return 算法模型列表
     */
    List<AlgorithmModel> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * 根据训练任务ID查询模型列表
     *
     * @param trainingId 训练任务ID
     * @return 算法模型列表
     */
    List<AlgorithmModel> selectByTrainingId(@Param("trainingId") Long trainingId);

    /**
     * 根据状态查询模型列表
     *
     * @param status 状态
     * @return 算法模型列表
     */
    List<AlgorithmModel> selectByStatus(@Param("status") String status);

    /**
     * 更新模型状态
     *
     * @param id 模型ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新模型下载次数
     *
     * @param id 模型ID
     * @return 影响行数
     */
    int updateDownloadCount(@Param("id") Long id);

    /**
     * 更新模型部署次数
     *
     * @param id 模型ID
     * @return 影响行数
     */
    int updateDeployCount(@Param("id") Long id);

    /**
     * 批量更新模型状态
     *
     * @param ids 模型ID列表
     * @param status 新状态
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 获取模型统计信息
     *
     * @return 统计信息
     */
    AlgorithmModelStatisticsDTO getStatistics();

    /**
     * 检查模型名称和版本是否存在
     *
     * @param modelName 模型名称
     * @param version 版本
     * @param excludeId 排除的ID（更新时使用）
     * @return 存在的数量
     */
    int checkModelNameAndVersion(@Param("modelName") String modelName, 
                                @Param("version") Integer version,
                                @Param("excludeId") Long excludeId);

    /**
     * 根据算法ID和版本查询模型
     *
     * @param algorithmId 算法ID
     * @param version 版本
     * @return 算法模型
     */
    AlgorithmModel selectByAlgorithmIdAndVersion(@Param("algorithmId") Long algorithmId, 
                                                @Param("version") Integer version);

    /**
     * 获取算法下最新版本的模型
     *
     * @param algorithmId 算法ID
     * @return 算法模型
     */
    AlgorithmModel selectLatestByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * 查询热门模型（按下载次数排序）
     *
     * @param limit 限制数量
     * @return 算法模型列表
     */
    List<AlgorithmModel> selectPopularModels(@Param("limit") Integer limit);

    /**
     * 根据创建人查询模型数量
     *
     * @param createdBy 创建人ID
     * @return 模型数量
     */
    Long countByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 获取算法模型的总大小（所有已发布模型的文件大小总和）
     *
     * @return 总大小（字节）
     */
    Long getTotalModelSize();
} 