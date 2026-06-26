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
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.ApiScope;
import org.springblade.modules.system.service.IApiScopeService;
import org.springblade.modules.system.vo.ApiScopeVO;
import org.springblade.modules.system.wrapper.ApiScopeWrapper;
import org.springframework.web.bind.annotation.*;

import static org.springblade.core.cache.constant.CacheConstant.SYS_CACHE;

/**
 * Interface permission controller
 *
 * @author BladeX
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/api-scope")
@Tag(name = "Interface permissions", description = "Interface permissions")
public class ApiScopeController extends BladeController {

	private final IApiScopeService apiScopeService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in dataScope")
	public R<ApiScope> detail(ApiScope dataScope) {
		ApiScope detail = apiScopeService.getOne(Condition.getQueryWrapper(dataScope));
		return R.data(detail);
	}

	/**
	 * Pagination
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in dataScope")
	public R<IPage<ApiScopeVO>> list(ApiScope dataScope, Query query) {
		IPage<ApiScope> pages = apiScopeService.page(Condition.getPage(query), Condition.getQueryWrapper(dataScope));
		return R.data(ApiScopeWrapper.build().pageVO(pages));
	}

	/**
	 * Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Add", description = "Pass in dataScope")
	public R save(@Valid @RequestBody ApiScope dataScope) {
		CacheUtil.clear(SYS_CACHE);
		return R.status(apiScopeService.save(dataScope));
	}

	/**
	 * Modify
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Modify", description = "Pass in dataScope")
	public R update(@Valid @RequestBody ApiScope dataScope) {
		CacheUtil.clear(SYS_CACHE);
		return R.status(apiScopeService.updateById(dataScope));
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add or modify", description = "Pass in dataScope")
	public R submit(@Valid @RequestBody ApiScope dataScope) {
		CacheUtil.clear(SYS_CACHE);
		return R.status(apiScopeService.saveOrUpdate(dataScope));
	}


	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		CacheUtil.clear(SYS_CACHE);
		return R.status(apiScopeService.deleteLogic(Func.toLongList(ids)));
	}

}
