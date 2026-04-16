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
 * Container Instance Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface ContainerInstanceMapper extends BaseMapper<ContainerInstance> {

    /**
     * Paginated query container instances (including associated information)
     *
     * @param page Pagination parameters
     * @param queryDTO Query conditions
     * @return Pagination result
     */
    IPage<ContainerInstance> selectPageWithDetails(IPage<ContainerInstance> page, @Param("query") ContainerInstanceQueryDTO queryDTO);

    /**
     * Query container instance details by ID (including associated information)
     *
     * @param id Container instance ID
     * @return Container instance details
     */
    ContainerInstance selectByIdWithDetails(@Param("id") Long id);

    /**
     * Query container instance by container ID
     *
     * @param containerId Container ID
     * @return Container instance
     */
    @Select("SELECT * FROM container_instance WHERE container_id = #{containerId} AND deleted = 0")
    ContainerInstance selectByContainerId(@Param("containerId") String containerId);

    /**
     * Query container instance by instance name (for duplicate name check)
     *
     * @param instanceName Instance name
     * @param excludeId Exclude ID (used for updates)
     * @return Container instance
     */
    @Select("<script>" +
            "SELECT * FROM container_instance WHERE instance_name = #{instanceName} AND deleted = 0" +
            "<if test='excludeId != null'> AND id != #{excludeId}</if>" +
            "</script>")
    ContainerInstance selectByInstanceName(@Param("instanceName") String instanceName, @Param("excludeId") Long excludeId);

    /**
     * Query container instance list by algorithm ID
     *
     * @param algorithmId Algorithm ID
     * @return Container instance list
     */
    @Select("SELECT * FROM container_instance WHERE algorithm_id = #{algorithmId} AND deleted = 0 ORDER BY created_time DESC")
    List<ContainerInstance> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

    /**
     * Query container instance list by status
     *
     * @param instanceStatus Instance status
     * @return Container instance list
     */
    @Select("SELECT * FROM container_instance WHERE instance_status = #{instanceStatus} AND deleted = 0")
    List<ContainerInstance> selectByStatus(@Param("instanceStatus") String instanceStatus);

    /**
     * Update container instance status
     *
     * @param id Container instance ID
     * @param instanceStatus Instance status
     * @param healthStatus Health status
     * @param containerId Container ID
     * @param startTime Start time
     * @param stopTime Stop time
     * @return Updated rows
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
     * Update container monitoring data
     *
     * @param id Container instance ID
     * @param cpuUsage CPU usage
     * @param memoryUsage Memory usage
     * @param gpuUsage GPU usage
     * @return Updated rows
     */
    @Update("UPDATE container_instance SET cpu_usage = #{cpuUsage}, " +
            "memory_usage = #{memoryUsage}, gpu_usage = #{gpuUsage}, updated_time = NOW() " +
            "WHERE id = #{id}")
    int updateMonitoringData(@Param("id") Long id,
                           @Param("cpuUsage") java.math.BigDecimal cpuUsage,
                           @Param("memoryUsage") java.math.BigDecimal memoryUsage,
                           @Param("gpuUsage") java.math.BigDecimal gpuUsage);

    /**
     * Increment restart count
     *
     * @param id Container instance ID
     * @return Updated rows
     */
    @Update("UPDATE container_instance SET restart_count = restart_count + 1, updated_time = NOW() WHERE id = #{id}")
    int increaseRestartCount(@Param("id") Long id);

    /**
     * Get container instance statistics
     *
     * @return Statistics result
     */
    Map<String, Object> selectStatistics();

    /**
     * Batch delete container instances
     *
     * @param ids Container instance ID list
     * @return Deleted rows
     */
    @Update("<script>" +
            "UPDATE container_instance SET deleted = 1, updated_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Long> ids);
} 