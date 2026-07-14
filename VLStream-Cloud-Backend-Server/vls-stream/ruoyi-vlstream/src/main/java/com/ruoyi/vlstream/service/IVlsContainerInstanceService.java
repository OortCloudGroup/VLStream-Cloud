/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.ContainerInstance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service for the VLS container instance frontend compatibility surface.
 */
public interface IVlsContainerInstanceService {

    /**
     * Return frontend-compatible paged container instances.
     */
    BladePage<ContainerInstance> getContainerInstancePage(Long current, Long size, String name, String status,
                                                          Long algorithmId, String healthStatus, String startTime,
                                                          String endTime);

    /**
     * Return a single container instance by ID.
     */
    ContainerInstance getContainerInstanceById(Long id);

    /**
     * Create a container instance from frontend aliases.
     */
    ContainerInstance createContainerInstance(ContainerInstance instance);

    /**
     * Update a container instance from frontend aliases.
     */
    ContainerInstance updateContainerInstance(ContainerInstance instance);

    /**
     * Soft delete one container instance.
     */
    boolean deleteContainerInstance(Long id);

    /**
     * Soft delete multiple container instances.
     */
    boolean batchDeleteContainerInstances(List<Long> ids);

    /**
     * Start a container instance through the local compatibility state transition.
     */
    boolean startContainerInstance(Long id);

    /**
     * Stop a container instance through the local compatibility state transition.
     */
    boolean stopContainerInstance(Long id);

    /**
     * Restart a container instance through the local compatibility state transition.
     */
    boolean restartContainerInstance(Long id);

    /**
     * Return aggregate container instance statistics.
     */
    Map<String, Object> getContainerInstanceStatistics();

    /**
     * Return running container instances.
     */
    List<ContainerInstance> getRunningContainerInstances();

    /**
     * Return error-state container instances.
     */
    List<ContainerInstance> getErrorContainerInstances();

    /**
     * Return unhealthy container instances.
     */
    List<ContainerInstance> getUnhealthyContainerInstances();

    /**
     * Check whether the instance name already exists.
     */
    boolean checkContainerInstanceName(String name, Long excludeId);

    /**
     * Return container instances for an algorithm.
     */
    List<ContainerInstance> getContainerInstancesByAlgorithm(Long algorithmId);

    /**
     * Update monitoring usage values.
     */
    boolean updateContainerInstanceMonitoring(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage);
}
