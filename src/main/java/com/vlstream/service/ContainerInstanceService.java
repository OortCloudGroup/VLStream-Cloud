package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.ContainerInstance;
import com.vlstream.dto.ContainerInstanceQueryDTO;
import com.vlstream.dto.ContainerInstanceCreateDTO;
import com.vlstream.dto.ContainerInstanceUpdateDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Container Instance Service Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface ContainerInstanceService extends IService<ContainerInstance> {

    /**
     * Query container instances with pagination
     *
     * @param page Pagination parameter
     * @param queryDTO Query conditions
     * @return Pagination result
     */
    IPage<ContainerInstance> pageContainerInstances(Page<ContainerInstance> page, ContainerInstanceQueryDTO queryDTO);

    /**
     * Query container instance details by ID
     *
     * @param id Container instance ID
     * @return Container instance details
     */
    ContainerInstance getContainerInstanceById(Long id);

    /**
     * Create container instance
     *
     * @param createDTO Create parameters
     * @return Created container instance
     */
    ContainerInstance createContainerInstance(ContainerInstanceCreateDTO createDTO);

    /**
     * Update container instance
     *
     * @param updateDTO Update parameters
     * @return Updated container instance
     */
    ContainerInstance updateContainerInstance(ContainerInstanceUpdateDTO updateDTO);

    /**
     * Delete container instance
     *
     * @param id Container instance ID
     * @return Whether deletion successful
     */
    boolean deleteContainerInstance(Long id);

    /**
     * Batch delete container instances
     *
     * @param ids Container instance ID list
     * @return Whether deletion successful
     */
    boolean deleteContainerInstanceBatch(List<Long> ids);

    /**
     * Query container instance by container ID
     *
     * @param containerId Container ID
     * @return Container instance
     */
    ContainerInstance getByContainerId(String containerId);

    /**
     * Query container instance list by algorithm ID
     *
     * @param algorithmId Algorithm ID
     * @return Container instance list
     */
    List<ContainerInstance> getByAlgorithmId(Long algorithmId);

    /**
     * Query container instance list by status
     *
     * @param instanceStatus Instance status
     * @return Container instance list
     */
    List<ContainerInstance> getByStatus(String instanceStatus);

    /**
     * Start container instance
     *
     * @param id Container instance ID
     * @param containerId Container ID
     * @return Whether start successful
     */
    boolean startContainer(Long id, String containerId);

    /**
     * Stop container instance
     *
     * @param id Container instance ID
     * @return Whether stop successful
     */
    boolean stopContainer(Long id);

    /**
     * Restart container instance
     *
     * @param id Container instance ID
     * @return Whether restart successful
     */
    boolean restartContainer(Long id);

    /**
     * Update container instance status
     *
     * @param id Container instance ID
     * @param instanceStatus Instance status
     * @param healthStatus Health status
     * @param containerId Container ID
     * @param startTime Start time
     * @param stopTime Stop time
     * @return Whether update successful
     */
    boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus, 
                               String containerId, LocalDateTime startTime, LocalDateTime stopTime);

    /**
     * Update container monitoring data
     *
     * @param id Container instance ID
     * @param cpuUsage CPU usage
     * @param memoryUsage Memory usage
     * @param gpuUsage GPU usage
     * @return Whether update successful
     */
    boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage);

    /**
     * Increase restart count
     *
     * @param id Container instance ID
     * @return Whether update successful
     */
    boolean increaseRestartCount(Long id);

    /**
     * Get container instance statistics
     *
     * @return Statistics result
     */
    Map<String, Object> getStatistics();

    /**
     * Check if instance name exists
     *
     * @param instanceName Instance name
     * @param excludeId Excluded ID (used for update)
     * @return Whether exists
     */
    boolean checkInstanceNameExists(String instanceName, Long excludeId);

    /**
     * Get running container instance list
     *
     * @return Running container instance list
     */
    List<ContainerInstance> getRunningInstances();

    /**
     * Get error status container instance list
     *
     * @return Error status container instance list
     */
    List<ContainerInstance> getErrorInstances();

    /**
     * Get unhealthy container instance list
     *
     * @return Unhealthy container instance list
     */
    List<ContainerInstance> getUnhealthyInstances();
} 