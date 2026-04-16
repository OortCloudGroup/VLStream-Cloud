package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.ContainerInstance;
import com.vlstream.mapper.ContainerInstanceMapper;
import com.vlstream.service.ContainerInstanceService;
import com.vlstream.dto.ContainerInstanceQueryDTO;
import com.vlstream.dto.ContainerInstanceCreateDTO;
import com.vlstream.dto.ContainerInstanceUpdateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Container Instance Service Implementation Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ContainerInstanceServiceImpl extends ServiceImpl<ContainerInstanceMapper, ContainerInstance> 
        implements ContainerInstanceService {

    @Override
    public IPage<ContainerInstance> pageContainerInstances(Page<ContainerInstance> page, ContainerInstanceQueryDTO queryDTO) {
        log.info("Page query container instances, current page: {}, page size: {}", page.getCurrent(), page.getSize());
        
        return baseMapper.selectPageWithDetails(page, queryDTO);
    }

    @Override
    public ContainerInstance getContainerInstanceById(Long id) {
        log.info("Query container instance details by ID: {}", id);
        
        ContainerInstance instance = baseMapper.selectByIdWithDetails(id);
        if (instance == null) {
            throw new RuntimeException("Container instance does not exist, ID: " + id);
        }
        
        return instance;
    }

    @Override
    @Transactional
    public ContainerInstance createContainerInstance(ContainerInstanceCreateDTO createDTO) {
        log.info("Create container instance, instance name: {}", createDTO.getInstanceName());
        
        // Check if instance name exists
        if (checkInstanceNameExists(createDTO.getInstanceName(), null)) {
            throw new RuntimeException("Instance name already exists: " + createDTO.getInstanceName());
        }
        
        // Create container instance
        ContainerInstance instance = new ContainerInstance();
        BeanUtils.copyProperties(createDTO, instance);
        
        // Set default values
        instance.setInstanceStatus("stopped");
        instance.setHealthStatus("unknown");
        instance.setRestartCount(0);
        
        // Save to database
        save(instance);
        
        log.info("Container instance created successfully, ID: {}", instance.getId());
        return instance;
    }

    @Override
    @Transactional
    public ContainerInstance updateContainerInstance(ContainerInstanceUpdateDTO updateDTO) {
        log.info("Update container instance, ID: {}", updateDTO.getId());
        
        ContainerInstance instance = getById(updateDTO.getId());
        if (instance == null) {
            throw new RuntimeException("Container instance does not exist, ID: " + updateDTO.getId());
        }
        
        // Check if instance name exists (exclude current instance)
        if (StringUtils.hasText(updateDTO.getInstanceName()) && 
            checkInstanceNameExists(updateDTO.getInstanceName(), updateDTO.getId())) {
            throw new RuntimeException("Instance name already exists: " + updateDTO.getInstanceName());
        }
        
        // Copy non-null properties
        if (StringUtils.hasText(updateDTO.getInstanceName())) {
            instance.setInstanceName(updateDTO.getInstanceName());
        }
        if (StringUtils.hasText(updateDTO.getContainerId())) {
            instance.setContainerId(updateDTO.getContainerId());
        }
        if (StringUtils.hasText(updateDTO.getInstanceStatus())) {
            instance.setInstanceStatus(updateDTO.getInstanceStatus());
        }
        if (StringUtils.hasText(updateDTO.getHealthStatus())) {
            instance.setHealthStatus(updateDTO.getHealthStatus());
        }
        if (updateDTO.getRestartCount() != null) {
            instance.setRestartCount(updateDTO.getRestartCount());
        }
        if (updateDTO.getCpuUsage() != null) {
            instance.setCpuUsage(updateDTO.getCpuUsage());
        }
        if (updateDTO.getMemoryUsage() != null) {
            instance.setMemoryUsage(updateDTO.getMemoryUsage());
        }
        if (updateDTO.getGpuUsage() != null) {
            instance.setGpuUsage(updateDTO.getGpuUsage());
        }
        if (StringUtils.hasText(updateDTO.getCpuLimit())) {
            instance.setCpuLimit(updateDTO.getCpuLimit());
        }
        if (StringUtils.hasText(updateDTO.getMemoryLimit())) {
            instance.setMemoryLimit(updateDTO.getMemoryLimit());
        }
        if (StringUtils.hasText(updateDTO.getGpuLimit())) {
            instance.setGpuLimit(updateDTO.getGpuLimit());
        }
        if (StringUtils.hasText(updateDTO.getPortConfig())) {
            instance.setPortConfig(updateDTO.getPortConfig());
        }
        if (StringUtils.hasText(updateDTO.getEnvConfig())) {
            instance.setEnvConfig(updateDTO.getEnvConfig());
        }
        if (StringUtils.hasText(updateDTO.getVolumeConfig())) {
            instance.setVolumeConfig(updateDTO.getVolumeConfig());
        }
        if (StringUtils.hasText(updateDTO.getLogsPath())) {
            instance.setLogsPath(updateDTO.getLogsPath());
        }
        
        // Update to database
        updateById(instance);
        
        log.info("Container instance updated successfully, ID: {}", instance.getId());
        return instance;
    }

    @Override
    @Transactional
    public boolean deleteContainerInstance(Long id) {
        log.info("Delete container instance, ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("Container instance does not exist, ID: " + id);
        }
        
        // Check if instance can be deleted
        if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
            throw new RuntimeException("Container instance is running, cannot delete");
        }
        
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean deleteContainerInstanceBatch(List<Long> ids) {
        log.info("Batch delete container instances, count: {}", ids.size());
        
        // Check if there are running instances
        List<ContainerInstance> instances = listByIds(ids);
        for (ContainerInstance instance : instances) {
            if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
                throw new RuntimeException("There are running container instances, cannot delete");
            }
        }
        
        return baseMapper.deleteBatch(ids) > 0;
    }

    @Override
    public ContainerInstance getByContainerId(String containerId) {
        return baseMapper.selectByContainerId(containerId);
    }

    @Override
    public List<ContainerInstance> getByAlgorithmId(Long algorithmId) {
        return baseMapper.selectByAlgorithmId(algorithmId);
    }

    @Override
    public List<ContainerInstance> getByStatus(String instanceStatus) {
        return baseMapper.selectByStatus(instanceStatus);
    }

    @Override
    @Transactional
    public boolean startContainer(Long id, String containerId) {
        log.info("Start container instance, ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("Container instance does not exist, ID: " + id);
        }
        
        if ("running".equals(instance.getInstanceStatus())) {
            throw new RuntimeException("Container instance is already running");
        }
        
        // Docker API should be called here to start the container
        // TODO: Integrate Docker API
        
        // Update status
        return updateInstanceStatus(id, "starting", "unknown", containerId, LocalDateTime.now(), null);
    }

    @Override
    @Transactional
    public boolean stopContainer(Long id) {
        log.info("Stop container instance, ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("Container instance does not exist, ID: " + id);
        }
        
        if ("stopped".equals(instance.getInstanceStatus())) {
            throw new RuntimeException("Container instance is already stopped");
        }
        
        // Docker API should be called here to stop the container
        // TODO: Integrate Docker API
        
        // Update status
        return updateInstanceStatus(id, "stopping", "unknown", instance.getContainerId(), 
                                  instance.getStartTime(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean restartContainer(Long id) {
        log.info("Restart container instance, ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("Container instance does not exist, ID: " + id);
        }
        
        // Docker API should be called here to restart the container
        // TODO: Integrate Docker API
        
        // Increase restart count
        increaseRestartCount(id);
        
        // Update status
        return updateInstanceStatus(id, "starting", "unknown", instance.getContainerId(), 
                                  LocalDateTime.now(), null);
    }

    @Override
    public boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus, 
                                      String containerId, LocalDateTime startTime, LocalDateTime stopTime) {
        return baseMapper.updateInstanceStatus(id, instanceStatus, healthStatus, containerId, startTime, stopTime) > 0;
    }

    @Override
    public boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage) {
        return baseMapper.updateMonitoringData(id, cpuUsage, memoryUsage, gpuUsage) > 0;
    }

    @Override
    public boolean increaseRestartCount(Long id) {
        return baseMapper.increaseRestartCount(id) > 0;
    }

    @Override
    public Map<String, Object> getStatistics() {
        return baseMapper.selectStatistics();
    }

    @Override
    public boolean checkInstanceNameExists(String instanceName, Long excludeId) {
        ContainerInstance instance = baseMapper.selectByInstanceName(instanceName, excludeId);
        return instance != null;
    }

    @Override
    public List<ContainerInstance> getRunningInstances() {
        return getByStatus("running");
    }

    @Override
    public List<ContainerInstance> getErrorInstances() {
        return getByStatus("error");
    }

    @Override
    public List<ContainerInstance> getUnhealthyInstances() {
        ContainerInstanceQueryDTO queryDTO = new ContainerInstanceQueryDTO();
        queryDTO.setHealthStatus("unhealthy");
        
        Page<ContainerInstance> page = new Page<>(1, Integer.MAX_VALUE);
        IPage<ContainerInstance> result = pageContainerInstances(page, queryDTO);
        return result.getRecords();
    }
} 