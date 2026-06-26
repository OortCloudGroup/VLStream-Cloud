package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.vlstream.excel.VlsContainerInstanceExcel;
import org.springblade.vlstream.pojo.entity.ContainerInstance;
import org.springblade.vlstream.pojo.vo.ContainerInstanceVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Container instance table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsContainerInstanceMapper extends BaseMapper<ContainerInstance> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsContainerInstance query parameters
	 * @return List<VlsContainerInstanceVO>
	 */
	List<ContainerInstanceVO> selectVlsContainerInstancePage(IPage page, ContainerInstanceVO vlsContainerInstance);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsContainerInstanceExcel>
	 */
	List<VlsContainerInstanceExcel> exportVlsContainerInstance(@Param("ew") Wrapper<ContainerInstance> queryWrapper);


	/**
	 * Query container instance details by ID (including association information)
	 *
	 * @param id container instance ID
	 * @return container instance details
	 */
	ContainerInstance selectByIdWithDetails(@Param("id") Long id);

	/**
	 * Query container instance by container ID
	 *
	 * @param containerId container ID
	 * @return container instance
	 */
	@Select("SELECT * FROM vls_container_instance WHERE container_id = #{containerId} AND is_deleted = 0")
	ContainerInstance selectByContainerId(@Param("containerId") String containerId);

	/**
	 * Query container instance by instance name (used for duplicate name check)
	 *
	 * @param instanceName instance name
	 * @param excludeId excluded ID (used when updating)
	 * @return container instance
	 */
	@Select("<script>" +
		"SELECT * FROM vls_container_instance WHERE instance_name = #{instanceName} AND is_deleted = 0" +
		"<if test='excludeId != null'> AND id != #{excludeId}</if>" +
		"</script>")
	ContainerInstance selectByInstanceName(@Param("instanceName") String instanceName, @Param("excludeId") Long excludeId);

	/**
	 * Query container instance list by algorithm ID
	 *
	 * @param algorithmId algorithm ID
	 * @return container instance list
	 */
	@Select("SELECT * FROM vls_container_instance WHERE algorithm_id = #{algorithmId} AND is_deleted = 0 ORDER BY create_time DESC")
	List<ContainerInstance> selectByAlgorithmId(@Param("algorithmId") Long algorithmId);

	/**
	 * Query container instance list by status
	 *
	 * @param instanceStatus instance status
	 * @return container instance list
	 */
	@Select("SELECT * FROM vls_container_instance WHERE instance_status = #{instanceStatus} AND is_deleted = 0")
	List<ContainerInstance> selectByStatus(@Param("instanceStatus") String instanceStatus);

	/**
	 * Update container instance status
	 *
	 * @param id container instance ID
	 * @param instanceStatus instance status
	 * @param healthStatus health status
	 * @param containerId container ID
	 * @param startTime start time
	 * @param stopTime stop time
	 * @return number of updated rows
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
	 * Update container monitoring data
	 *
	 * @param id container instance ID
	 * @param cpuUsage CPU usage
	 * @param memoryUsage memory usage
	 * @param gpuUsage GPU usage
	 * @return number of updated rows
	 */
	@Update("UPDATE vls_container_instance SET cpu_usage = #{cpuUsage}, " +
		"memory_usage = #{memoryUsage}, gpu_usage = #{gpuUsage}, update_time = NOW() " +
		"WHERE id = #{id}")
	int updateMonitoringData(@Param("id") Long id,
							 @Param("cpuUsage") java.math.BigDecimal cpuUsage,
							 @Param("memoryUsage") java.math.BigDecimal memoryUsage,
							 @Param("gpuUsage") java.math.BigDecimal gpuUsage);

	/**
	 * Increase restart count
	 *
	 * @param id container instance ID
	 * @return number of updated rows
	 */
	@Update("UPDATE vls_container_instance SET restart_count = restart_count + 1, update_time = NOW() WHERE id = #{id}")
	int increaseRestartCount(@Param("id") Long id);

	/**
	 * Get container instance statistics
	 *
	 * @return statistical results
	 */
	Map<String, Object> selectStatistics();

	/**
	 * Batch delete container instances
	 *
	 * @param ids container instance ID list
	 * @return number of deleted rows
	 */
	@Update("<script>" +
		"UPDATE vls_container_instance SET deleted = 1, update_time = NOW() WHERE id IN " +
		"<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
		"#{id}" +
		"</foreach>" +
		"</script>")
	int deleteBatch(@Param("ids") List<Long> ids);

}
