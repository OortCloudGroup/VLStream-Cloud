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
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceTagRelationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceTagRelation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceTagRelationVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceTagRelationService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsDeviceTagRelationWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * device control
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsDeviceTagRelation")
@Tag(name = "设备标签关联表", description = "设备标签关联表接口")
public class VlsDeviceTagRelationController extends BladeController {

	private final IVlsDeviceTagRelationService vlsDeviceTagRelationService;

	/**
	 * device
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsDeviceTagRelation")
	public R<DeviceTagRelationVO> detail(DeviceTagRelation vlsDeviceTagRelation) {
		DeviceTagRelation detail = vlsDeviceTagRelationService.getOne(Condition.getQueryWrapper(vlsDeviceTagRelation));
		return R.data(VlsDeviceTagRelationWrapper.build().entityVO(detail));
	}

	/**
	 * device
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsDeviceTagRelation")
	public R<IPage<DeviceTagRelationVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsDeviceTagRelation, Query query) {
		IPage<DeviceTagRelation> pages = vlsDeviceTagRelationService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsDeviceTagRelation, DeviceTagRelation.class));
		return R.data(VlsDeviceTagRelationWrapper.build().pageVO(pages));
	}


	/**
	 * device Custom
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsDeviceTagRelation")
	public R<IPage<DeviceTagRelationVO>> page(DeviceTagRelationVO vlsDeviceTagRelation, Query query) {
		IPage<DeviceTagRelationVO> pages = vlsDeviceTagRelationService.selectVlsDeviceTagRelationPage(Condition.getPage(query), vlsDeviceTagRelation);
		return R.data(pages);
	}

	/**
	 * device Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsDeviceTagRelation")
	public R save(@Valid @RequestBody DeviceTagRelation vlsDeviceTagRelation) {
		return R.status(vlsDeviceTagRelationService.save(vlsDeviceTagRelation));
	}

	/**
	 * device Update
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsDeviceTagRelation")
	public R update(@Valid @RequestBody DeviceTagRelation vlsDeviceTagRelation) {
		return R.status(vlsDeviceTagRelationService.updateById(vlsDeviceTagRelation));
	}

	/**
	 * device Add Update
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsDeviceTagRelation")
	public R submit(@Valid @RequestBody DeviceTagRelation vlsDeviceTagRelation) {
		return R.status(vlsDeviceTagRelationService.saveOrUpdate(vlsDeviceTagRelation));
	}

	/**
	 * device Delete
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsDeviceTagRelationService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsDeviceTagRelation")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsDeviceTagRelation")
	public void exportVlsDeviceTagRelation(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsDeviceTagRelation, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<DeviceTagRelation> queryWrapper = Condition.getQueryWrapper(vlsDeviceTagRelation, DeviceTagRelation.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsDeviceTagRelationEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsDeviceTagRelationEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsDeviceTagRelationExcel> list = vlsDeviceTagRelationService.exportVlsDeviceTagRelation(queryWrapper);
		ExcelUtil.export(response, "设备标签关联表数据" + DateUtil.today(), "设备标签关联表数据表", list, VlsDeviceTagRelationExcel.class);
	}

}
