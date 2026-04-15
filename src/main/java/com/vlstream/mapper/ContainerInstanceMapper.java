package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vlstream.entity.ContainerInstance;
import com.vlstream.dto.ContainerInstanceQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 容器实例Mapper接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface ContainerInstanceMapper extends BaseMapper<ContainerInstance> {

    /**
     * 分页查询容器实例（包含关联信息）
     *
     * @param page 分页参数
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<ContainerInstance> selectPageWithDetails(IPage<ContainerInstance> page, @Param("query") ContainerInstanceQueryDTO queryDTO);

    /**
     * 根据ID查询容器实例详情（包含关联信息）
     *
     * @param id 容器实例ID
     * @return 容器实例详情
     */
    ContainerInstance selectByIdWithDetails(@Param("id") Long id);

    /**
     * 根据容器ID查询容器实例
     *
     * @param containerId 容器ID
     * @return 容器实例
     */
    @Select("SELECT * FROM container_instance WHERE container_id = #{containerId} AND deleted = 0")
    ContainerInstance selectByContainerId(@Param("containerId") String containerId);

    /**
     * 根据实例名称查询容器实例（用于重名检查）
     *
     * @param instanceName 实例名称
     * @param excludeId 排除的ID（更新时使用）
     * @return 容器实例
     */
    @Select("<script>" +
            "SELECT * FROM container_instance WHERE instance_name = #{instanceName} AND deleted = 0" +
            "<if test='excludeId != null'> AND id != #{excludeId}</if>" +
            "</script>")
    ContainerInstance selectByInstanceName(@Param("instanceName") String instanceName, @Param("excludeId") Long excludeId);

    /**
     * 根据算法ID查询容器实例列表
     *
     * @param algorithmId 算法ID
     * @return 容器实例列表
     */
    @Select("SELECT * FROM container_instance WHERE algorithm_id = #{algorithmId} AND deleted = 0 ORDER BY created_time DESC")
    List<ContainerInstance> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * 根据状态查询容器实例列表
     *
     * @param instanceStatus 实例状态
     * @return 容器实例列表
     */
    @Select("SELECT * FROM container_instance WHERE instance_status = #{instanceStatus} AND deleted = 0")
    List<ContainerInstance> selectByStatus(@Param("instanceStatus") String instanceStatus);

    /**
     * 更新容器实例状态
     *
     * @param id 容器实例ID
     * @param instanceStatus 实例状态
     * @param healthStatus 健康状态
     * @param containerId 容器ID
     * @param startTime 启动时间
     * @param stopTime 停止时间
     * @return 更新行数
     */
    @Update("UPDATE container_instance SET instance_status = #{instanceStatus}, " +
            "health_status = #{healthStatus}, container_id = #{containerId}, " +
            "start_time = #{startTime}, stop_time = #{stopTime}, updated_time = NOW() " +
            "WHERE id = #{id}")
    int updateInstanceStatus(@Param("id") Long id, 
                           @Param("instanceStatus") String instanceStatus,
                           @Param("healthStatus") String healthStatus,
                           @Param("containerId") String containerId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("stopTime") LocalDateTime stopTime);

    /**
     * 更新容器监控数据
     *
     * @param id 容器实例ID
     * @param cpuUsage CPU使用率
     * @param memoryUsage 内存使用率
     * @param gpuUsage GPU使用率
     * @return 更新行数
     */
    @Update("UPDATE container_instance SET cpu_usage = #{cpuUsage}, " +
            "memory_usage = #{memoryUsage}, gpu_usage = #{gpuUsage}, updated_time = NOW() " +
            "WHERE id = #{id}")
    int updateMonitoringData(@Param("id") Long id,
                           @Param("cpuUsage") java.math.BigDecimal cpuUsage,
                           @Param("memoryUsage") java.math.BigDecimal memoryUsage,
                           @Param("gpuUsage") java.math.BigDecimal gpuUsage);

    /**
     * 增加重启次数
     *
     * @param id 容器实例ID
     * @return 更新行数
     */
    @Update("UPDATE container_instance SET restart_count = restart_count + 1, updated_time = NOW() WHERE id = #{id}")
    int increaseRestartCount(@Param("id") Long id);

    /**
     * 获取容器实例统计信息
     *
     * @return 统计结果
     */
    Map<String, Object> selectStatistics();

    /**
     * 批量删除容器实例
     *
     * @param ids 容器实例ID列表
     * @return 删除行数
     */
    @Update("<script>" +
            "UPDATE container_instance SET deleted = 1, updated_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Long> ids);
} 