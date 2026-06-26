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
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Param;
import org.springblade.modules.system.service.IParamService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller
 *
 * @author Chill
 */
@Hidden
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/param")
@Tag(name = "Parameter Management", description = "Interface")
public class ParamController extends BladeController {

	private IParamService paramService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@Operation(summary = "Details", description = "Pass in param")
	public R<Param> detail(Param param) {
		Param detail = paramService.getOne(Condition.getQueryWrapper(param));
		return R.data(detail);
	}

	/**
	 * Pagination
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "paramName", description = "Parameter name", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "paramKey", description = "Parameter key name", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "paramValue", description = "Parameter key value", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@Operation(summary = "Pagination", description = "Pass in param")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<IPage<Param>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> param, Query query) {
		IPage<Param> pages = paramService.page(Condition.getPage(query), Condition.getQueryWrapper(param, Param.class));
		return R.data(pages);
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@Operation(summary = "Add or modify", description = "Pass in param")
	public R submit(@Valid @RequestBody Param param) {
		return R.status(paramService.saveOrUpdate(param));
	}


	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(paramService.deleteLogic(Func.toLongList(ids)));
	}


}
