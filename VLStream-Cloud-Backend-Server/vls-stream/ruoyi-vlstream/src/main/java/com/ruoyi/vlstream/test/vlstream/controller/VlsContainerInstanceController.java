/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.excel.VlsContainerInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsContainerInstanceWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * instance control
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/vlsContainerInstance")
@Tag(name = "容器实例表", description = "容器实例表接口")
public class VlsContainerInstanceController extends BladeController {

	private final IVlsContainerInstanceService vlsContainerInstanceService;
	private final GpuTrainingSchedulerService gpuTrainingSchedulerService;
	@javax.annotation.Resource private com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess publicAccess;

	@GetMapping("/resources")
	@Operation(summary = "GPU服务器资源", description = "读取当前SSH训练服务器和GPU实时资源")
	public R<Map<String, Object>> resources() {
		return R.data(gpuTrainingSchedulerService.getResourceSnapshot());
	}

	@GetMapping("/{id}")
	@Operation(summary = "容器任务详情")
	public R<ContainerInstance> getById(@PathVariable Long id) {
		if(publicAccess!=null)publicAccess.container(id);
		return R.data(vlsContainerInstanceService.getById(id));
	}

	@GetMapping("/{id}/logs")
	@Operation(summary = "训练容器日志")
	public R<String> logs(@PathVariable Long id,
						  @RequestParam(defaultValue = "500") Integer lines) {
		if(publicAccess!=null)publicAccess.container(id);
		return R.data(gpuTrainingSchedulerService.getContainerLogs(id, lines));
	}

	private void guardCloudRecord(ContainerInstance input) {
		if(publicAccess!=null){publicAccess.container(input.getId());publicAccess.training(input.getTrainingTaskId());}
		ContainerInstance existing = input.getId() == null ? null : vlsContainerInstanceService.getById(input.getId());
		if(input.getId()!=null)com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess.protectContainerUpdate(existing,input);
		else if(publicAccess!=null)publicAccess.prepareContainerCreate(input);
		if ("cloud_training".equals(input.getInstanceType()) || (existing != null && "cloud_training".equals(existing.getInstanceType()))) {
			throw new com.ruoyi.common.exception.ServiceException("线上训练记录由后台维护，请从算法训练页面管理任务");
		}
	}

	/**
	 * instance
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description  = "传入vlsContainerInstance")
	public R<ContainerInstanceVO> detail(ContainerInstance vlsContainerInstance) {
		QueryWrapper<ContainerInstance> wrapper=Condition.getQueryWrapper(vlsContainerInstance);
		if(publicAccess!=null)publicAccess.filterContainers(wrapper);
		ContainerInstance detail = vlsContainerInstanceService.getOne(wrapper);
		return R.data(VlsContainerInstanceWrapper.build().entityVO(detail));
	}

	/**
	 * instance
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description  = "传入vlsContainerInstance")
	public R<IPage<ContainerInstanceVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsContainerInstance, Query query) {
		QueryWrapper<ContainerInstance> wrapper=Condition.getQueryWrapper(vlsContainerInstance,ContainerInstance.class);
		if(publicAccess!=null)publicAccess.filterContainers(wrapper);
		IPage<ContainerInstance> pages = vlsContainerInstanceService.page(Condition.getPage(query),wrapper);
		return R.data(VlsContainerInstanceWrapper.build().pageVO(pages));
	}


	/**
	 * instance Custom
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description  = "传入vlsContainerInstance")
	public R<IPage<ContainerInstanceVO>> page(ContainerInstanceVO vlsContainerInstance, Query query) {
		IPage<ContainerInstanceVO> pages = vlsContainerInstanceService.selectVlsContainerInstancePage(Condition.getPage(query), vlsContainerInstance);
		return R.data(pages);
	}

	/**
	 * instance Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description  = "传入vlsContainerInstance")
	public R save(@Valid @RequestBody ContainerInstance vlsContainerInstance) {
		guardCloudRecord(vlsContainerInstance);
		return R.status(vlsContainerInstanceService.save(vlsContainerInstance));
	}

	/**
	 * instance Update
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description  = "传入vlsContainerInstance")
	public R update(@Valid @RequestBody ContainerInstance vlsContainerInstance) {
		guardCloudRecord(vlsContainerInstance);
		return R.status(vlsContainerInstanceService.updateById(vlsContainerInstance));
	}

	/**
	 * instance Add Update
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description  = "传入vlsContainerInstance")
	public R submit(@Valid @RequestBody ContainerInstance vlsContainerInstance) {
		guardCloudRecord(vlsContainerInstance);
		return R.status(vlsContainerInstanceService.saveOrUpdate(vlsContainerInstance));
	}

	/**
	 * instance Delete
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description  = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		for (Long id : Func.toLongList(ids)) {
			ContainerInstance record = new ContainerInstance(); record.setId(id); guardCloudRecord(record);
		}
		return R.status(vlsContainerInstanceService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsContainerInstance")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description  = "传入vlsContainerInstance")
	public void exportVlsContainerInstance(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsContainerInstance, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<ContainerInstance> queryWrapper = Condition.getQueryWrapper(vlsContainerInstance, ContainerInstance.class);
		if(publicAccess!=null)publicAccess.filterContainers(queryWrapper);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsContainerInstanceEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsContainerInstanceEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsContainerInstanceExcel> list = vlsContainerInstanceService.exportVlsContainerInstance(queryWrapper);
		ExcelUtil.export(response, "容器实例表数据" + DateUtil.today(), "容器实例表数据表", list, VlsContainerInstanceExcel.class);
	}

}
