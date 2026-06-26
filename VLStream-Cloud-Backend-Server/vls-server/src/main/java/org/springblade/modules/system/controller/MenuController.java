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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Menu;
import org.springblade.modules.system.entity.TopMenu;
import org.springblade.modules.system.service.IMenuService;
import org.springblade.modules.system.service.ITopMenuService;
import org.springblade.modules.system.vo.CheckedTreeVO;
import org.springblade.modules.system.vo.GrantTreeVO;
import org.springblade.modules.system.vo.MenuVO;
import org.springblade.modules.system.wrapper.MenuWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/menu")
@Tag(name = "Menu", description = "Menu")
public class MenuController extends BladeController {

	private final IMenuService menuService;

	private final ITopMenuService topMenuService;

	/**
	 * Details
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in menu")
	public R<MenuVO> detail(Menu menu) {
		Menu detail = menuService.getOne(Condition.getQueryWrapper(menu));
		return R.data(MenuWrapper.build().entityVO(detail));
	}

	/**
	 * List
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "code", description = "Menu code", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "Menu name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 2)
	@Operation(summary = "List", description = "Pass in menu")
	public R<List<MenuVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<Menu> list = menuService.list(Condition.getQueryWrapper(menu, Menu.class).lambda().orderByAsc(Menu::getSort));
		return R.data(MenuWrapper.build().listNodeVO(list));
	}

	/**
	 * Menu list
	 */
	@GetMapping("/menu-list")
	@Parameters({
		@Parameter(name = "code", description = "Menu code", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "Menu name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Menu list", description = "Pass in menu")
	public R<List<MenuVO>> menuList(@Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<Menu> list = menuService.list(Condition.getQueryWrapper(menu, Menu.class).lambda().eq(Menu::getCategory, 1).orderByAsc(Menu::getSort));
		return R.data(MenuWrapper.build().listNodeVO(list));
	}

	/**
	 * Lazy loaded menu list
	 */
	@GetMapping("/lazy-menu-list")
	@Parameters({
		@Parameter(name = "code", description = "Menu code", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "Menu name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Lazy loaded menu list", description = "Pass in menu")
	public R<List<MenuVO>> lazyMenuList(Long parentId, @Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<MenuVO> list = menuService.lazyMenuList(parentId, menu);
		return R.data(MenuWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add or modify", description = "Pass in menu")
	public R submit(@Valid @RequestBody Menu menu) {
		return R.status(menuService.saveOrUpdate(menu));
	}


	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(menuService.removeByIds(Func.toLongList(ids)));
	}

	/**
	 * Front-end menu data
	 */
	@GetMapping("/routes")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Front-end menu data", description = "Front-end menu data")
	public R<List<MenuVO>> routes(BladeUser user, Long topMenuId) {
		List<MenuVO> list = menuService.routes((user == null || user.getUserId() == 0L) ? null : user.getRoleId(), topMenuId);
		return R.data(list);
	}

	/**
	 * Front-end button data
	 */
	@GetMapping("/buttons")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Front-end button data", description = "Front-end button data")
	public R<List<MenuVO>> buttons(BladeUser user) {
		List<MenuVO> list = menuService.buttons(user.getRoleId());
		return R.data(list);
	}

	/**
	 * Get menu tree structure
	 */
	@GetMapping("/tree")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "Tree Structure", description = "Tree Structure")
	public R<List<MenuVO>> tree() {
		List<MenuVO> tree = menuService.tree();
		return R.data(tree);
	}

	/**
	 * Get permission assignment tree structure
	 */
	@GetMapping("/grant-tree")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "Permission Assignment Tree Structure", description = "Permission Assignment Tree Structure")
	public R<GrantTreeVO> grantTree(BladeUser user) {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(menuService.grantTree(user));
		vo.setDataScope(menuService.grantDataScopeTree(user));
		vo.setApiScope(menuService.grantApiScopeTree(user));
		return R.data(vo);
	}

	/**
	 * Get permission assignment tree structure
	 */
	@GetMapping("/role-tree-keys")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "Tree allocated to role", description = "Tree allocated to role")
	public R<CheckedTreeVO> roleTreeKeys(String roleIds) {
		CheckedTreeVO vo = new CheckedTreeVO();
		vo.setMenu(menuService.roleTreeKeys(roleIds));
		vo.setDataScope(menuService.dataScopeTreeKeys(roleIds));
		vo.setApiScope(menuService.apiScopeTreeKeys(roleIds));
		return R.data(vo);
	}

	/**
	 * Get configured role permissions
	 */
	@GetMapping("auth-routes")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "Role permissions of menu")
	public R<List<Kv>> authRoutes(BladeUser user) {
		if (Func.isEmpty(user) || user.getUserId() == 0L) {
			return null;
		}
		return R.data(menuService.authRoutes(user));
	}

	/**
	 * Top menu data
	 */
	@GetMapping("/top-menu")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "Top menu data", description = "Top menu data")
	public R<List<TopMenu>> topMenu(BladeUser user) {
		if (Func.isEmpty(user)) {
			return null;
		}
		List<TopMenu> list = topMenuService.list(Wrappers.<TopMenu>query().lambda().orderByAsc(TopMenu::getSort));
		return R.data(list);
	}

	/**
	 * Get top menu tree structure
	 */
	@GetMapping("/grant-top-tree")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 14)
	@Operation(summary = "Top menu tree structure", description = "Top menu tree structure")
	public R<GrantTreeVO> grantTopTree(BladeUser user) {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(menuService.grantTopTree(user));
		return R.data(vo);
	}

	/**
	 * Get top menu tree structure
	 */
	@GetMapping("/top-tree-keys")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@ApiOperationSupport(order = 15)
	@Operation(summary = "Tree allocated to top menu", description = "Tree allocated to top menu")
	public R<CheckedTreeVO> topTreeKeys(String topMenuIds) {
		CheckedTreeVO vo = new CheckedTreeVO();
		vo.setMenu(menuService.topTreeKeys(topMenuIds));
		return R.data(vo);
	}

}
