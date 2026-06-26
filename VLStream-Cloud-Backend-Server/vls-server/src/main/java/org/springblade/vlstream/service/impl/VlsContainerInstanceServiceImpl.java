package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsContainerInstanceExcel;
import org.springblade.vlstream.mapper.VlsContainerInstanceMapper;
import org.springblade.vlstream.pojo.entity.ContainerInstance;
import org.springblade.vlstream.pojo.vo.ContainerInstanceVO;
import org.springblade.vlstream.service.IVlsContainerInstanceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Container instance table service implementation class
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
		log.info("Delete container instance, ID: {}", id);

		ContainerInstance instance = getById(id);
		if (instance == null) {
			throw new RuntimeException("Container instance does not exist, ID:" + id);
		}

		// Check if it can be deleted
		if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
			throw new RuntimeException("Container instance is running, deletion is not allowed");
		}

		return removeById(id);
	}

	@Override
	@Transactional
	public boolean deleteContainerInstanceBatch(List<Long> ids) {
		log.info("Batch delete container instances, count: {}", ids.size());

		// Check if there are any running instances
		List<ContainerInstance> instances = listByIds(ids);
		for (ContainerInstance instance : instances) {
			if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
				throw new RuntimeException("Running container instances exist, deletion is not allowed");
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
			throw new RuntimeException("Container instance does not exist, ID:" + id);
		}

		if ("running".equals(instance.getInstanceStatus())) {
			throw new RuntimeException("Container instance is already running");
		}

		// Docker API should be called here to start container
		// TODO: Integrate Docker API

		// Update status
		return updateInstanceStatus(id, "starting", "unknown", containerId, new Date(), null);
	}

	@Override
	@Transactional
	public boolean stopContainer(Long id) {
		log.info("Stop container instance, ID: {}", id);

		ContainerInstance instance = getById(id);
		if (instance == null) {
			throw new RuntimeException("Container instance does not exist, ID:" + id);
		}

		if ("stopped".equals(instance.getInstanceStatus())) {
			throw new RuntimeException("Container instance stopped");
		}

		// Docker API should be called here to stop container
		// TODO: Integrate Docker API

		// Update status
		return updateInstanceStatus(id, "stopping", "unknown", instance.getContainerId(),
			instance.getStartTime(), new Date());
	}

	@Override
	@Transactional
	public boolean restartContainer(Long id) {
		log.info("Restart container instance, ID: {}", id);

		ContainerInstance instance = getById(id);
		if (instance == null) {
			throw new RuntimeException("Container instance does not exist, ID:" + id);
		}

		// Docker API should be called here to restart container
		// TODO: Integrate Docker API

		// Increase restart count
		increaseRestartCount(id);

		// Update status
		return updateInstanceStatus(id, "starting", "unknown", instance.getContainerId(),
			new Date(), null);
	}

	@Override
	public boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus,
										String containerId, Date startTime, Date stopTime) {
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

}
