package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.vlstream.pojo.entity.ResourceType;
import org.springblade.vlstream.pojo.vo.ResourceTypeVO;
import org.springblade.vlstream.service.IVlsResourceTypeService;
import org.springblade.vlstream.wrapper.VlsResourceTypeWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Resource Type Configuration Table Controller
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsResourceType")
@Tag(name = "Resource Type Configuration", description = "Resource Type Configuration Interface")
public class VlsResourceTypeController extends BladeController {

	private final IVlsResourceTypeService vlsResourceTypeService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in resourceType")
	public R<ResourceTypeVO> detail(ResourceType resourceType) {
		ResourceType detail = vlsResourceTypeService.getOne(Condition.getQueryWrapper(resourceType));
		return R.data(VlsResourceTypeWrapper.build().entityVO(detail));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in resourceType")
	public R<IPage<ResourceTypeVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> resourceType, Query query) {
		IPage<ResourceType> pages = vlsResourceTypeService.page(Condition.getPage(query), Condition.getQueryWrapper(resourceType, ResourceType.class));
		return R.data(VlsResourceTypeWrapper.build().pageVO(pages));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Add", description = "Pass in resourceType")
	public R save(@Valid @RequestBody ResourceType resourceType) {
		return R.status(vlsResourceTypeService.save(resourceType));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Modify", description = "Pass in resourceType")
	public R update(@Valid @RequestBody ResourceType resourceType) {
		return R.status(vlsResourceTypeService.updateById(resourceType));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add or modify", description = "Pass in resourceType")
	public R submit(@Valid @RequestBody ResourceType resourceType) {
		return R.status(vlsResourceTypeService.saveOrUpdate(resourceType));
	}

	@GetMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsResourceTypeService.deleteLogic(Func.toLongList(ids)));
	}
}
