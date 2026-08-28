/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsContainerInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * instance Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsContainerInstanceMapper extends BaseMapper<ContainerInstance> {

	/**
	 * Scheduler-only cross-tenant scan for active training containers.
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM vls_container_instance WHERE is_deleted = 0 "
		+ "AND instance_type = 'training' AND instance_status IN ('starting', 'running') ORDER BY id")
	List<ContainerInstance> selectActiveTrainingForScheduler();

	/**
	 * Scheduler-only cross-tenant lookup for the oldest queued training task.
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM vls_container_instance WHERE is_deleted = 0 "
		+ "AND instance_type = 'training' AND instance_status = 'queued' "
		+ "ORDER BY queue_time ASC, id ASC LIMIT 1")
	ContainerInstance selectNextQueuedTrainingForScheduler();

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsContainerInstance Query parameter
	 * @return List<VlsContainerInstanceVO>
	 */
	List<ContainerInstanceVO> selectVlsContainerInstancePage(IPage page,
															 @Param("query") ContainerInstanceVO vlsContainerInstance);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsContainerInstanceExcel>
	 */
	List<VlsContainerInstanceExcel> exportVlsContainerInstance(@Param("ew") Wrapper<ContainerInstance> queryWrapper);


	/**
	 * IDQuery instance ( info)
	 *
	 * @param id instanceID
	 * @return instance
	 */
	ContainerInstance selectByIdWithDetails(@Param("id") Long id);

	/**
	 * IDQuery instance
	 *
	 * @param containerId ID
	 * @return instance
	 */
	@Select("SELECT * FROM vls_container_instance WHERE container_id = #{containerId} AND is_deleted = 0")
	ContainerInstance selectByContainerId(@Param("containerId") String containerId);

	/**
	 * instance Query instance ( )
	 *
	 * @param instanceName instance
	 * @param excludeId ID ( new )
	 * @return instance
	 */
	@Select("<script>" +
		"SELECT * FROM vls_container_instance WHERE instance_name = #{instanceName} AND is_deleted = 0" +
		"<if test='excludeId != null'> AND id != #{excludeId}</if>" +
		"</script>")
	ContainerInstance selectByInstanceName(@Param("instanceName") String instanceName, @Param("excludeId") Long excludeId);

	/**
	 * algorithmIDQuery instance list
	 *
	 * @param algorithmId algorithmID
	 * @return instance
	 */
	@Select("SELECT * FROM vls_container_instance WHERE algorithm_id = #{algorithmId} AND is_deleted = 0 ORDER BY create_time DESC")
	List<ContainerInstance> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * Query instance list
	 *
	 * @param instanceStatus instance
	 * @return instance
	 */
	@Select("SELECT * FROM vls_container_instance WHERE instance_status = #{instanceStatus} AND is_deleted = 0")
	List<ContainerInstance> selectByStatus(@Param("instanceStatus") String instanceStatus);

	/**
	 * new instance
	 *
	 * @param id instanceID
	 * @param instanceStatus instance
	 * @param healthStatus
	 * @param containerId ID
	 * @param startTime
	 * @param stopTime
	 * @return new
	 */
	@Update("UPDATE vls_container_instance SET instance_status = #{instanceStatus}, " +
		"health_status = #{healthStatus}, container_id = #{containerId}, " +
		"start_time = #{startTime}, stop_time = #{stopTime}, update_time = NOW() " +
		"WHERE id = #{id}")
	int updateInstanceStatus(@Param("id") Long id,
							 @Param("instanceStatus") String instanceStatus,
							 @Param("healthStatus") String healthStatus,
							 @Param("containerId") String containerId,
							 @Param("startTime") Date startTime,
							 @Param("stopTime") Date stopTime);

	/**
	 * new data
	 *
	 * @param id instanceID
	 * @param cpuUsage CPU
	 * @param memoryUsage
	 * @param gpuUsage GPU
	 * @return new
	 */
	@Update("UPDATE vls_container_instance SET cpu_usage = #{cpuUsage}, " +
		"memory_usage = #{memoryUsage}, gpu_usage = #{gpuUsage}, update_time = NOW() " +
		"WHERE id = #{id}")
	int updateMonitoringData(@Param("id") Long id,
							 @Param("cpuUsage") java.math.BigDecimal cpuUsage,
							 @Param("memoryUsage") java.math.BigDecimal memoryUsage,
							 @Param("gpuUsage") java.math.BigDecimal gpuUsage);

	/**
	 *
	 *
	 * @param id instanceID
	 * @return new
	 */
	@Update("UPDATE vls_container_instance SET restart_count = restart_count + 1, update_time = NOW() WHERE id = #{id}")
	int increaseRestartCount(@Param("id") Long id);

	/**
	 * Get instance info
	 *
	 * @return
	 */
	Map<String, Object> selectStatistics();

	/**
	 * Batch delete instance
	 *
	 * @param ids instanceID
	 * @return Delete
	 */
	@Update("<script>" +
		"UPDATE vls_container_instance SET deleted = 1, update_time = NOW() WHERE id IN " +
		"<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
		"#{id}" +
		"</foreach>" +
		"</script>")
	int deleteBatch(@Param("ids") List<Long> ids);

}
