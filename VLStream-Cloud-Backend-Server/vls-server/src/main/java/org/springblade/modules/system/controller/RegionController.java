/**
 * Copyright (c) 2018-2099, Chill Zhuang (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.entity.Region;
import org.springblade.modules.system.service.IRegionService;
import org.springblade.modules.system.vo.RegionVO;
import org.springblade.modules.system.vo.RoleVO;
import org.springblade.modules.system.wrapper.RegionWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Administrative division table controller
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/region")
@Tag(name = "Administrative division table", description = "Administrative division table interface")
public class RegionController extends BladeController {

	private final IRegionService regionService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in region")
	public R<RegionVO> detail(Region region) {
		Region detail = regionService.getOne(Condition.getQueryWrapper(region));
		return R.data(RegionWrapper.build().entityVO(detail));
	}

	/**
	 * Paginate administrative division table
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in region")
	public R<IPage<Region>> list(Region region, Query query) {
		IPage<Region> pages = regionService.page(Condition.getPage(query), Condition.getQueryWrapper(region));
		return R.data(pages);
	}

	/**
	 * Lazy loaded list
	 */
	@GetMapping("/lazy-list")
	@Parameters({
		@Parameter(name = "code", description = "Division code", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "Division name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Lazy loaded list", description = "Pass in menu")
	public R<List<RoleVO>> lazyList(String parentCode, @Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<RoleVO> list = regionService.lazyList(parentCode, menu);
		return R.data(RegionWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * Lazy loaded list
	 */
	@GetMapping("/lazy-tree")
	@Parameters({
		@Parameter(name = "code", description = "Division code", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "Division name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Lazy loaded list", description = "Pass in menu")
	public R<List<RoleVO>> lazyTree(String parentCode, @Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<RoleVO> list = regionService.lazyTree(parentCode, menu);
		return R.data(RegionWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * Add Administrative Division Table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add", description = "Pass in region")
	public R save(@Valid @RequestBody Region region) {
		return R.status(regionService.save(region));
	}

	/**
	 * Modify administrative division table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Modify", description = "Pass in region")
	public R update(@Valid @RequestBody Region region) {
		return R.status(regionService.updateById(region));
	}

	/**
	 * Add or modify Administrative Division Table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Add or modify", description = "Pass in region")
	public R submit(@Valid @RequestBody Region region) {
		return R.status(regionService.submit(region));
	}


	/**
	 * Delete administrative division table
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Delete", description = "Pass in primary key")
	public R remove(@Parameter(description = "Primary key", required = true) @RequestParam String id) {
		return R.status(regionService.removeRegion(id));
	}

	/**
	 * Administrative division dropdown data source
	 */
	@GetMapping("/select")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "Dropdown data source", description = "Pass in tenant")
	public R<List<Region>> select(@RequestParam(required = false, defaultValue = "00") String code) {
		List<Region> list = regionService.list(Wrappers.<Region>query().lambda().eq(Region::getParentCode, code));
		return R.data(list);
	}


}
