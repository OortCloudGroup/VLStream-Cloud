/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsContainerInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * instance service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsContainerInstanceService extends BaseService<ContainerInstance> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsContainerInstance Query parameter
	 * @return IPage<VlsContainerInstanceVO>
	 */
	IPage<ContainerInstanceVO> selectVlsContainerInstancePage(IPage<ContainerInstanceVO> page, ContainerInstanceVO vlsContainerInstance);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsContainerInstanceExcel>
	 */
	List<VlsContainerInstanceExcel> exportVlsContainerInstance(Wrapper<ContainerInstance> queryWrapper);

	/**
	 * Delete instance
	 *
	 * @param id instanceID
	 * @return whether Delete successfully
	 */
	boolean deleteContainerInstance(Long id);

	/**
	 * Batch delete instance
	 *
	 * @param ids instanceID
	 * @return whether Delete successfully
	 */
	boolean deleteContainerInstanceBatch(List<Long> ids);

	/**
	 * IDQuery instance
	 *
	 * @param containerId ID
	 * @return instance
	 */
	ContainerInstance getByContainerId(String containerId);

	/**
	 * algorithmIDQuery instance list
	 *
	 * @param algorithmId algorithmID
	 * @return instance
	 */
	List<ContainerInstance> getByAlgorithmId(Long algorithmId);

	/**
	 * Query instance list
	 *
	 * @param instanceStatus instance
	 * @return instance
	 */
	List<ContainerInstance> getByStatus(String instanceStatus);

	/**
	 * instance
	 *
	 * @param id instanceID
	 * @param containerId ID
	 * @return whether successfully
	 */
	boolean startContainer(Long id, String containerId);

	/**
	 * instance
	 *
	 * @param id instanceID
	 * @return whether successfully
	 */
	boolean stopContainer(Long id);

	/**
	 * instance
	 *
	 * @param id instanceID
	 * @return whether successfully
	 */
	boolean restartContainer(Long id);

	/**
	 * new instance
	 *
	 * @param id instanceID
	 * @param instanceStatus instance
	 * @param healthStatus
	 * @param containerId ID
	 * @param startTime
	 * @param stopTime
	 * @return whether new successfully
	 */
	boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus,
								 String containerId, Date startTime, Date stopTime);

	/**
	 * new data
	 *
	 * @param id instanceID
	 * @param cpuUsage CPU
	 * @param memoryUsage
	 * @param gpuUsage GPU
	 * @return whether new successfully
	 */
	boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage);

	/**
	 *
	 *
	 * @param id instanceID
	 * @return whether new successfully
	 */
	boolean increaseRestartCount(Long id);

	/**
	 * Get instance info
	 *
	 * @return
	 */
	Map<String, Object> getStatistics();

	/**
	 * instance whether
	 *
	 * @param instanceName instance
	 * @param excludeId ID ( new )
	 * @return whether
	 */
	boolean checkInstanceNameExists(String instanceName, Long excludeId);

	/**
	 * Get in instance
	 *
	 * @return in instance
	 */
	List<ContainerInstance> getRunningInstances();

	/**
	 * Get instance
	 *
	 * @return instance
	 */
	List<ContainerInstance> getErrorInstances();

}
