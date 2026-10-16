/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * instance service
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsContainerInstanceServiceImpl extends BaseServiceImpl<VlsContainerInstanceMapper, ContainerInstance> implements IVlsContainerInstanceService {
	@javax.annotation.Resource private com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess publicAccess;
	@Override public ContainerInstance getById(java.io.Serializable id) {
		if(publicAccess!=null && id!=null)publicAccess.containerFromHttp(Long.valueOf(id.toString()));
		return super.getById(id);
	}
	@Override public boolean save(ContainerInstance row) {
		if(publicAccess!=null && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()!=null)
			publicAccess.prepareContainerCreate(row);
		return super.save(row);
	}
	@Override public boolean saveTaskRecord(ContainerInstance row) {
		if(publicAccess==null)throw new com.ruoyi.common.exception.ServiceException("缺少可信任务记录创建上下文");
		publicAccess.prepareTaskRecord(row);
		// The three schedulers construct this record, after persisted owner/type validation.
		// Do not clear HTTP context or route this through manual DTO creation.
		return super.save(row);
	}
	@Override public void requireTaskExecution(ContainerInstance row) {
		if(publicAccess==null)throw new com.ruoyi.common.exception.ServiceException("缺少可信调度上下文");
		publicAccess.requireTaskExecution(row);
	}
	@Override public boolean updateById(ContainerInstance row) {
		if(publicAccess!=null && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()!=null) {
			if(row.getId()==null)throw new com.ruoyi.common.exception.ServiceException("缺少容器记录身份");
			publicAccess.container(row.getId());
			com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess.protectContainerUpdate(super.getById(row.getId()),row);
		}
		return super.updateById(row);
	}

	@Override
	public IPage<ContainerInstanceVO> selectVlsContainerInstancePage(IPage<ContainerInstanceVO> page, ContainerInstanceVO vlsContainerInstance) {
		return page.setRecords(baseMapper.selectVlsContainerInstancePage(page, vlsContainerInstance,publicAccess==null?"":publicAccess.viewer()));
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

		// whether Delete
		if ("running".equals(instance.getInstanceStatus()) || "starting".equals(instance.getInstanceStatus())) {
			throw new RuntimeException("容器实例正在运行中，无法删除");
		}

		return removeById(id);
	}

	@Override
	@Transactional
	public boolean deleteContainerInstanceBatch(List<Long> ids) {
		log.info("批量删除容器实例，数量: {}", ids.size());

		// whether in instance
		if(publicAccess!=null)ids.forEach(publicAccess::containerFromHttp);
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
		ContainerInstance row=baseMapper.selectByContainerId(containerId);
		if(publicAccess!=null && row!=null)publicAccess.containerFromHttp(row.getId());
		return row;
	}

	@Override
	public List<ContainerInstance> getByAlgorithmId(Long algorithmId) {
		return baseMapper.selectList(visibleContainers().eq("algorithm_id",algorithmId).orderByDesc("create_time"));
	}

	@Override
	public List<ContainerInstance> getByStatus(String instanceStatus) {
		return baseMapper.selectList(visibleContainers().eq("instance_status",instanceStatus));
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

		// Docker API
		// TODO: Docker API

		// new
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

		// Docker API
		// TODO: Docker API

		// new
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

		// Docker API
		// TODO: Docker API

		//
		increaseRestartCount(id);

		// new
		return updateInstanceStatus(id, "starting", "unknown", instance.getContainerId(),
			new Date(), null);
	}

	@Override
	public boolean updateInstanceStatus(Long id, String instanceStatus, String healthStatus,
										String containerId, Date startTime, Date stopTime) {
		if(publicAccess!=null)publicAccess.containerFromHttp(id);
		ContainerInstance instance = new ContainerInstance();
		instance.setId(id);
		instance.setInstanceStatus(instanceStatus);
		instance.setHealthStatus(healthStatus);
		instance.setContainerId(containerId);
		instance.setStartTime(startTime);
		instance.setStopTime(stopTime);
		if(publicAccess!=null && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()!=null)
			com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess.protectContainerExecution(super.getById(id),instance);
		// Explicit owned lifecycle operations are distinct from generic client DTO updates.
		return super.updateById(instance);
	}

	@Override
	public boolean updateMonitoringData(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage) {
		if(publicAccess!=null)publicAccess.containerFromHttp(id);
		ContainerInstance instance = new ContainerInstance();
		instance.setId(id);
		instance.setCpuUsage(cpuUsage);
		instance.setMemoryUsage(memoryUsage);
		instance.setGpuUsage(gpuUsage);
		return updateById(instance);
	}

	@Override
	public boolean increaseRestartCount(Long id) {
		if(publicAccess!=null)publicAccess.containerFromHttp(id);
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
	private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ContainerInstance> visibleContainers() {
		com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ContainerInstance> query=new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
		query.eq("is_deleted",0);
		if(publicAccess!=null && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()!=null)publicAccess.filterContainers(query);
		return query;
	}

}
