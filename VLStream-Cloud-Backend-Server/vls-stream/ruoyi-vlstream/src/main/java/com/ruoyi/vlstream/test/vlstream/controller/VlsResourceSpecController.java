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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ResourceSpec;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ResourceSpecVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsResourceSpecService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsResourceSpecWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资源规格配置表 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsResourceSpec")
@Tag(name = "资源规格配置", description = "资源规格配置接口")
public class VlsResourceSpecController extends BladeController {

	private final IVlsResourceSpecService vlsResourceSpecService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入resourceSpec")
	public R<ResourceSpecVO> detail(ResourceSpec resourceSpec) {
		ResourceSpec detail = vlsResourceSpecService.getOne(Condition.getQueryWrapper(resourceSpec));
		return R.data(VlsResourceSpecWrapper.build().entityVO(detail));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入resourceSpec")
	public R<IPage<ResourceSpecVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> resourceSpec, Query query) {
		IPage<ResourceSpec> pages = vlsResourceSpecService.page(Condition.getPage(query), Condition.getQueryWrapper(resourceSpec, ResourceSpec.class));
		return R.data(VlsResourceSpecWrapper.build().pageVO(pages));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增", description = "传入resourceSpec")
	public R save(@Valid @RequestBody ResourceSpec resourceSpec) {
		return R.status(vlsResourceSpecService.save(resourceSpec));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "修改", description = "传入resourceSpec")
	public R update(@Valid @RequestBody ResourceSpec resourceSpec) {
		return R.status(vlsResourceSpecService.updateById(resourceSpec));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "传入resourceSpec")
	public R submit(@Valid @RequestBody ResourceSpec resourceSpec) {
		return R.status(vlsResourceSpecService.saveOrUpdate(resourceSpec));
	}

	@GetMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsResourceSpecService.deleteLogic(Func.toLongList(ids)));
	}
}
