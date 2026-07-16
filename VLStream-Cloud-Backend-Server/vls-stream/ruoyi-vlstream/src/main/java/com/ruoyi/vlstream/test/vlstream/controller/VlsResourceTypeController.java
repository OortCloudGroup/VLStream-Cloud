/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ResourceType;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ResourceTypeVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsResourceTypeService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsResourceTypeWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资源类型配置表 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsResourceType")
@Tag(name = "资源类型配置", description = "资源类型配置接口")
public class VlsResourceTypeController extends BladeController {

	private final IVlsResourceTypeService vlsResourceTypeService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入resourceType")
	public R<ResourceTypeVO> detail(ResourceType resourceType) {
		ResourceType detail = vlsResourceTypeService.getOne(Condition.getQueryWrapper(resourceType));
		return R.data(VlsResourceTypeWrapper.build().entityVO(detail));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入resourceType")
	public R<IPage<ResourceTypeVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> resourceType, Query query) {
		IPage<ResourceType> pages = vlsResourceTypeService.page(Condition.getPage(query), Condition.getQueryWrapper(resourceType, ResourceType.class));
		return R.data(VlsResourceTypeWrapper.build().pageVO(pages));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增", description = "传入resourceType")
	public R save(@Valid @RequestBody ResourceType resourceType) {
		return R.status(vlsResourceTypeService.save(resourceType));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "修改", description = "传入resourceType")
	public R update(@Valid @RequestBody ResourceType resourceType) {
		return R.status(vlsResourceTypeService.updateById(resourceType));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "传入resourceType")
	public R submit(@Valid @RequestBody ResourceType resourceType) {
		return R.status(vlsResourceTypeService.saveOrUpdate(resourceType));
	}

	@GetMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsResourceTypeService.deleteLogic(Func.toLongList(ids)));
	}
}
