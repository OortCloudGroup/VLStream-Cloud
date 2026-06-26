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
package org.springblade.modules.develop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
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
import org.springblade.develop.support.BladeCodeGenerator;
import org.springblade.modules.develop.entity.Code;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.service.ICodeService;
import org.springblade.modules.develop.service.IDatasourceService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Controller
 *
 * @author Chill
 */
@Hidden
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_DEVELOP_NAME + "/code")
@Tag(name = "Code generation", description = "Code generation")
@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
public class CodeController extends BladeController {

	private ICodeService codeService;
	private IDatasourceService datasourceService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in code")
	public R<Code> detail(Code code) {
		Code detail = codeService.getOne(Condition.getQueryWrapper(code));
		return R.data(detail);
	}

	/**
	 * Pagination
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "codeName", description = "Module name", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "tableName", description = "Table name", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "modelName", description = "Entity name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in code")
	public R<IPage<Code>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> code, Query query) {
		IPage<Code> pages = codeService.page(Condition.getPage(query), Condition.getQueryWrapper(code, Code.class));
		return R.data(pages);
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Add or modify", description = "Pass in code")
	public R submit(@Valid @RequestBody Code code) {
		return R.status(codeService.submit(code));
	}


	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(codeService.removeByIds(Func.toLongList(ids)));
	}

	/**
	 * Copy
	 */
	@PostMapping("/copy")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Copy", description = "Pass in id")
	public R copy(@Parameter(description = "Primary key", required = true) @RequestParam Long id) {
		Code code = codeService.getById(id);
		code.setId(null);
		code.setCodeName(code.getCodeName() + "-copy");
		return R.status(codeService.save(code));
	}

	/**
	 * Code generation
	 */
	@PostMapping("/gen-code")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Code generation", description = "Pass in ids")
	public R genCode(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids, @RequestParam(defaultValue = "saber3") String system) {
		Collection<Code> codes = codeService.listByIds(Func.toLongList(ids));
		codes.forEach(code -> {
			BladeCodeGenerator generator = new BladeCodeGenerator();
			// Set Data Source
			Datasource datasource = datasourceService.getById(code.getDatasourceId());
			generator.setDriverName(datasource.getDriverClass());
			generator.setUrl(datasource.getUrl());
			generator.setUsername(datasource.getUsername());
			generator.setPassword(datasource.getPassword());
			// Set basic configurations
			generator.setSystemName(system);
			generator.setServiceName(code.getServiceName());
			generator.setPackageName(code.getPackageName());
			generator.setPackageDir(code.getApiPath());
			generator.setPackageWebDir(code.getWebPath());
			generator.setTablePrefix(Func.toStrArray(code.getTablePrefix()));
			generator.setIncludeTables(Func.toStrArray(code.getTableName()));
			// Set whether to inherit basic business fields
			generator.setHasSuperEntity(code.getBaseMode() == 2);
			// Controller adds service name prefix
			generator.setHasServiceName(Boolean.TRUE);
			// Set whether to enable wrapper mode
			generator.setHasWrapper(code.getWrapMode() == 2);
			generator.run();
		});
		return R.success("Code generation succeeded");
	}

}
