package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsContainerInstanceExcel;
import org.springblade.vlstream.pojo.entity.ContainerInstance;
import org.springblade.vlstream.pojo.vo.ContainerInstanceVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Container instance table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsContainerInstanceService extends BaseService<ContainerInstance> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsContainerInstance query parameters
	 * @return IPage<VlsContainerInstanceVO>
	 */
	IPage<ContainerInstanceVO> selectVlsContainerInstancePage(IPage<ContainerInstanceVO> page, ContainerInstanceVO vlsContainerInstance);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsContainerInstanceExcel>
	 */
	List<VlsContainerInstanceExcel> exportVlsContainerInstance(Wrapper<ContainerInstance> queryWrapper);

	/**
	 * Delete container instance
	 *
	 * @param id container instance ID
	 * @return whether deleted successfully
	 */
	boolean deleteContainerInstance(Long id);

	/**
	 * Batch delete container instances
	 *
	 * @param ids container instance ID list
	 * @return whether deleted successfully
	 */
	boolean deleteContainerInstanceBatch(List<Long> ids);

	/**
	 * Query container instance by container ID
	 *
	 * @param containerId container ID
	 * @return container instance
	 */
	ContainerInstance getByContainerId(String containerId);

	/**
	 * Query container instance list by algorithm ID
	 *
	 * @param algorithmId algorithm ID
	 * @return container instance list
	 */
	List<ContainerInstance> getByAlgorithmId(Long algorithmId);

	/**
	 * Query container instance list by status
	 *
	 * @param instanceStatus instance status
	 * @return container instance list
	 */
	List<ContainerInstance> getByStatus(String instanceStatus);

	/**
	 * Start container instance
	 *
	 * @param id container instance ID
	 * @param containerId container ID
	 * @return whether started successfully
	 */
	boolean startContainer(Long id, String containerId);

	/**
	 * Stop container instance
	 *
	 * @param id container instance ID
	 * @return whether stopped successfully
	 */
	boolean stopContainer(Long id);

	/**
	 * Restart container instance
	 *
	 * @param id container instance ID
	 * @return whether restarted successfully
	 */
	boolean restartContainer(Long id);

	/**
	 * Update container instance status
	 *
	 * @param id container instance ID
	 * @param instanceStatus instance status
	 * @param healthStatus health status
	 * @param containerId container ID
	 * @param startTime start time
	 * @param stopTime stop time
	 * @return whether updated successfully
	 */
	boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus,
								 String containerId, Date startTime, Date stopTime);

	/**
	 * Update container monitoring data
	 *
	 * @param id container instance ID
	 * @param cpuUsage CPU usage
	 * @param memoryUsage memory usage
	 * @param gpuUsage GPU usage
	 * @return whether updated successfully
	 */
	boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage);

	/**
	 * Increase restart count
	 *
	 * @param id container instance ID
	 * @return whether updated successfully
	 */
	boolean increaseRestartCount(Long id);

	/**
	 * Get container instance statistics
	 *
	 * @return statistical results
	 */
	Map<String, Object> getStatistics();

	/**
	 * Check if instance name is duplicated
	 *
	 * @param instanceName instance name
	 * @param excludeId excluded ID (used when updating)
	 * @return whether duplicated
	 */
	boolean checkInstanceNameExists(String instanceName, Long excludeId);

	/**
	 * Get running container instances
	 *
	 * @return List of running container instances
	 */
	List<ContainerInstance> getRunningInstances();

	/**
	 * Get container instances with abnormal status
	 *
	 * @return list of container instances in abnormal state
	 */
	List<ContainerInstance> getErrorInstances();

}
