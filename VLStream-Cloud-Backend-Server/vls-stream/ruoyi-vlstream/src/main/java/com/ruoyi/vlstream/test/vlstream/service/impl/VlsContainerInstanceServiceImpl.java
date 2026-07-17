/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsContainerInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsContainerInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 容器实例表 服务实现类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsContainerInstanceServiceImpl extends BaseServiceImpl<VlsContainerInstanceMapper, ContainerInstance> implements IVlsContainerInstanceService {

	@Override
	public IPage<ContainerInstanceVO> selectVlsContainerInstancePage(IPage<ContainerInstanceVO> page, ContainerInstanceVO vlsContainerInstance) {
		return page.setRecords(baseMapper.selectVlsContainerInstancePage(page, vlsContainerInstance));
	}

	@Override
	public List<VlsContainerInstanceExcel> exportVlsContainerInstance(Wrapper<ContainerInstance> queryWrapper) {
		List<VlsContainerInstanceExcel> vlsContainerInstanceList = baseMapper.exportVlsContainerInstance(queryWrapper);
		//vlsContainerInstanceList.forEach(vlsContainerInstance -> {
		//	vlsContainerInstance.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsContainerInstanceEntity.getType()));
		//});
		return vlsContainerInstanceList;
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
		return updateInstanceStatus(id, "starting", "unknown", containerId, new Date(), null);
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
			instance.getStartTime(), new Date());
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
			new Date(), null);
	}

	@Override
	public boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus,
										String containerId, Date startTime, Date stopTime) {
		ContainerInstance instance = new ContainerInstance();
		instance.setId(id);
		instance.setInstanceStatus(instanceStatus);
		instance.setHealthStatus(healthStatus);
		instance.setContainerId(containerId);
		instance.setStartTime(startTime);
		instance.setStopTime(stopTime);
		return updateById(instance);
	}

	@Override
	public boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage) {
		ContainerInstance instance = new ContainerInstance();
		instance.setId(id);
		instance.setCpuUsage(cpuUsage);
		instance.setMemoryUsage(memoryUsage);
		instance.setGpuUsage(gpuUsage);
		return updateById(instance);
	}

	@Override
	public boolean increaseRestartCount(Long id) {
		UpdateWrapper<ContainerInstance> wrapper = new UpdateWrapper<>();
		wrapper.eq("id", id).setSql("restart_count = restart_count + 1");
		return update(new ContainerInstance(), wrapper);
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

}
