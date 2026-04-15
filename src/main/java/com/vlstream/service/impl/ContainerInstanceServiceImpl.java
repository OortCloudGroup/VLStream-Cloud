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
 * 容器实例服务实现类
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
        log.info("分页查询容器实例，当前页: {}，每页大小: {}", page.getCurrent(), page.getSize());
        
        return baseMapper.selectPageWithDetails(page, queryDTO);
    }

    @Override
    public ContainerInstance getContainerInstanceById(Long id) {
        log.info("根据ID查询容器实例详情，ID: {}", id);
        
        ContainerInstance instance = baseMapper.selectByIdWithDetails(id);
        if (instance == null) {
            throw new RuntimeException("容器实例不存在，ID: " + id);
        }
        
        return instance;
    }

    @Override
    @Transactional
    public ContainerInstance createContainerInstance(ContainerInstanceCreateDTO createDTO) {
        log.info("创建容器实例，实例名称: {}", createDTO.getInstanceName());
        
        // 检查实例名称是否重复
        if (checkInstanceNameExists(createDTO.getInstanceName(), null)) {
            throw new RuntimeException("实例名称已存在: " + createDTO.getInstanceName());
        }
        
        // 创建容器实例
        ContainerInstance instance = new ContainerInstance();
        BeanUtils.copyProperties(createDTO, instance);
        
        // 设置默认值
        instance.setInstanceStatus("stopped");
        instance.setHealthStatus("unknown");
        instance.setRestartCount(0);
        
        // 保存到数据库
        save(instance);
        
        log.info("容器实例创建成功，ID: {}", instance.getId());
        return instance;
    }

    @Override
    @Transactional
    public ContainerInstance updateContainerInstance(ContainerInstanceUpdateDTO updateDTO) {
        log.info("更新容器实例，ID: {}", updateDTO.getId());
        
        ContainerInstance instance = getById(updateDTO.getId());
        if (instance == null) {
            throw new RuntimeException("容器实例不存在，ID: " + updateDTO.getId());
        }
        
        // 检查实例名称是否重复（排除当前实例）
        if (StringUtils.hasText(updateDTO.getInstanceName()) && 
            checkInstanceNameExists(updateDTO.getInstanceName(), updateDTO.getId())) {
            throw new RuntimeException("实例名称已存在: " + updateDTO.getInstanceName());
        }
        
        // 复制非空属性
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
        
        // 更新到数据库
        updateById(instance);
        
        log.info("容器实例更新成功，ID: {}", instance.getId());
        return instance;
    }

    @Override
    @Transactional
    public boolean deleteContainerInstance(Long id) {
        log.info("删除容器实例，ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("容器实例不存在，ID: " + id);
        }
        
        // 检查是否可以删除
        if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
            throw new RuntimeException("容器实例正在运行中，无法删除");
        }
        
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean deleteContainerInstanceBatch(List<Long> ids) {
        log.info("批量删除容器实例，数量: {}", ids.size());
        
        // 检查是否有运行中的实例
        List<ContainerInstance> instances = listByIds(ids);
        for (ContainerInstance instance : instances) {
            if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
                throw new RuntimeException("存在运行中的容器实例，无法删除");
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
        log.info("启动容器实例，ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("容器实例不存在，ID: " + id);
        }
        
        if ("running".equals(instance.getInstanceStatus())) {
            throw new RuntimeException("容器实例已在运行中");
        }
        
        // 这里应该调用Docker API启动容器
        // TODO: 集成Docker API
        
        // 更新状态
        return updateInstanceStatus(id, "starting", "unknown", containerId, LocalDateTime.now(), null);
    }

    @Override
    @Transactional
    public boolean stopContainer(Long id) {
        log.info("停止容器实例，ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("容器实例不存在，ID: " + id);
        }
        
        if ("stopped".equals(instance.getInstanceStatus())) {
            throw new RuntimeException("容器实例已停止");
        }
        
        // 这里应该调用Docker API停止容器
        // TODO: 集成Docker API
        
        // 更新状态
        return updateInstanceStatus(id, "stopping", "unknown", instance.getContainerId(), 
                                  instance.getStartTime(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean restartContainer(Long id) {
        log.info("重启容器实例，ID: {}", id);
        
        ContainerInstance instance = getById(id);
        if (instance == null) {
            throw new RuntimeException("容器实例不存在，ID: " + id);
        }
        
        // 这里应该调用Docker API重启容器
        // TODO: 集成Docker API
        
        // 增加重启次数
        increaseRestartCount(id);
        
        // 更新状态
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