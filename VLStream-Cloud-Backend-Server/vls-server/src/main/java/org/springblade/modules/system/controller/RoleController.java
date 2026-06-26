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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
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
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Role;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.vo.GrantVO;
import org.springblade.modules.system.vo.RoleVO;
import org.springblade.modules.system.wrapper.RoleWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springblade.core.cache.utils.CacheUtil.SYS_CACHE;


/**
 * Controller
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/role")
@Tag(name = "Role", description = "Role")
public class RoleController extends BladeController {

	private IRoleService roleService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in role")
	public R<RoleVO> detail(Role role) {
		Role detail = roleService.getOne(Condition.getQueryWrapper(role));
		return R.data(RoleWrapper.build().entityVO(detail));
	}

	/**
	 * List
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "roleName", description = "Parameter name", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "roleAlias", description = "Role Alias", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 2)
	@Operation(summary = "List", description = "Pass in role")
	public R<List<RoleVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> role, BladeUser bladeUser) {
		QueryWrapper<Role> queryWrapper = Condition.getQueryWrapper(role, Role.class);
		List<Role> list = roleService.list((!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ? queryWrapper.lambda().eq(Role::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(RoleWrapper.build().listNodeVO(list));
	}

	/**
	 * Get role tree structure
	 */
	@GetMapping("/tree")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Tree Structure", description = "Tree Structure")
	public R<List<RoleVO>> tree(String tenantId, BladeUser bladeUser) {
		List<RoleVO> tree = roleService.tree(Func.toStr(tenantId, bladeUser.getTenantId()));
		return R.data(tree);
	}

	/**
	 * Get specified role tree structure
	 */
	@GetMapping("/tree-by-id")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Tree Structure", description = "Tree Structure")
	public R<List<RoleVO>> treeById(Long roleId, BladeUser bladeUser) {
		Role role = roleService.getById(roleId);
		List<RoleVO> tree = roleService.tree(Func.notNull(role) ? role.getTenantId() : bladeUser.getTenantId());
		return R.data(tree);
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add or modify", description = "Pass in role")
	public R submit(@Valid @RequestBody Role role, BladeUser user) {
		CacheUtil.clear(SYS_CACHE);
		if (Func.isEmpty(role.getId())) {
			role.setTenantId(user.getTenantId());
		}
		return R.status(roleService.saveOrUpdate(role));
	}

	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		CacheUtil.clear(SYS_CACHE);
		return R.status(roleService.removeByIds(Func.toLongList(ids)));
	}

	/**
	 * Set Role Permissions
	 */
	@PostMapping("/grant")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Permission Settings", description = "Pass in roleId collection and menuId collection")
	public R grant(@RequestBody GrantVO grantVO) {
		CacheUtil.clear(SYS_CACHE);
		boolean temp = roleService.grant(grantVO.getRoleIds(), grantVO.getMenuIds(), grantVO.getDataScopeIds(), grantVO.getApiScopeIds());
		return R.status(temp);
	}

}
