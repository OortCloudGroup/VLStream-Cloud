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
 * 容器实例服务接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface ContainerInstanceService extends IService<ContainerInstance> {

    /**
     * 分页查询容器实例
     *
     * @param page 分页参数
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<ContainerInstance> pageContainerInstances(Page<ContainerInstance> page, ContainerInstanceQueryDTO queryDTO);

    /**
     * 根据ID查询容器实例详情
     *
     * @param id 容器实例ID
     * @return 容器实例详情
     */
    ContainerInstance getContainerInstanceById(Long id);

    /**
     * 创建容器实例
     *
     * @param createDTO 创建参数
     * @return 创建的容器实例
     */
    ContainerInstance createContainerInstance(ContainerInstanceCreateDTO createDTO);

    /**
     * 更新容器实例
     *
     * @param updateDTO 更新参数
     * @return 更新后的容器实例
     */
    ContainerInstance updateContainerInstance(ContainerInstanceUpdateDTO updateDTO);

    /**
     * 删除容器实例
     *
     * @param id 容器实例ID
     * @return 是否删除成功
     */
    boolean deleteContainerInstance(Long id);

    /**
     * 批量删除容器实例
     *
     * @param ids 容器实例ID列表
     * @return 是否删除成功
     */
    boolean deleteContainerInstanceBatch(List<Long> ids);

    /**
     * 根据容器ID查询容器实例
     *
     * @param containerId 容器ID
     * @return 容器实例
     */
    ContainerInstance getByContainerId(String containerId);

    /**
     * 根据算法ID查询容器实例列表
     *
     * @param algorithmId 算法ID
     * @return 容器实例列表
     */
    List<ContainerInstance> getByAlgorithmId(Long algorithmId);

    /**
     * 根据状态查询容器实例列表
     *
     * @param instanceStatus 实例状态
     * @return 容器实例列表
     */
    List<ContainerInstance> getByStatus(String instanceStatus);

    /**
     * 启动容器实例
     *
     * @param id 容器实例ID
     * @param containerId 容器ID
     * @return 是否启动成功
     */
    boolean startContainer(Long id, String containerId);

    /**
     * 停止容器实例
     *
     * @param id 容器实例ID
     * @return 是否停止成功
     */
    boolean stopContainer(Long id);

    /**
     * 重启容器实例
     *
     * @param id 容器实例ID
     * @return 是否重启成功
     */
    boolean restartContainer(Long id);

    /**
     * 更新容器实例状态
     *
     * @param id 容器实例ID
     * @param instanceStatus 实例状态
     * @param healthStatus 健康状态
     * @param containerId 容器ID
     * @param startTime 启动时间
     * @param stopTime 停止时间
     * @return 是否更新成功
     */
    boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus, 
                               String containerId, LocalDateTime startTime, LocalDateTime stopTime);

    /**
     * 更新容器监控数据
     *
     * @param id 容器实例ID
     * @param cpuUsage CPU使用率
     * @param memoryUsage 内存使用率
     * @param gpuUsage GPU使用率
     * @return 是否更新成功
     */
    boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage);

    /**
     * 增加重启次数
     *
     * @param id 容器实例ID
     * @return 是否更新成功
     */
    boolean increaseRestartCount(Long id);

    /**
     * 获取容器实例统计信息
     *
     * @return 统计结果
     */
    Map<String, Object> getStatistics();

    /**
     * 检查实例名称是否重复
     *
     * @param instanceName 实例名称
     * @param excludeId 排除的ID（更新时使用）
     * @return 是否重复
     */
    boolean checkInstanceNameExists(String instanceName, Long excludeId);

    /**
     * 获取运行中的容器实例列表
     *
     * @return 运行中的容器实例列表
     */
    List<ContainerInstance> getRunningInstances();

    /**
     * 获取异常状态的容器实例列表
     *
     * @return 异常状态的容器实例列表
     */
    List<ContainerInstance> getErrorInstances();

    /**
     * 获取健康状态不正常的容器实例列表
     *
     * @return 健康状态不正常的容器实例列表
     */
    List<ContainerInstance> getUnhealthyInstances();
} 