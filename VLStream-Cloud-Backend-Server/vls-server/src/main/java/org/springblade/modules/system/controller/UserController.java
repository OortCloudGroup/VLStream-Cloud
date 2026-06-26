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


import cn.idev.excel.FastExcel;
import cn.idev.excel.read.builder.ExcelReaderBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.common.cache.CacheNames;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.excel.UserExcel;
import org.springblade.modules.system.excel.UserImportListener;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.system.vo.UserVO;
import org.springblade.modules.system.wrapper.UserWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller
 *
 * @author Chill
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/user")
@AllArgsConstructor
@Tag(name = "User table", description = "User table interface")
public class UserController {

	private IUserService userService;
	private BladeRedis bladeRedis;

	/**
	 * Query Single Record
	 */
	@ApiOperationSupport(order = 1)
	@Operation(summary = "View Details", description = "Pass in id")
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<UserVO> detail(User user) {
		User detail = userService.getOne(Condition.getQueryWrapper(user));
		return R.data(UserWrapper.build().entityVO(detail));
	}

	/**
	 * Query Single Record
	 */
	@ApiOperationSupport(order = 2)
	@Operation(summary = "View Details", description = "Pass in id")
	@GetMapping("/info")
	public R<UserVO> info(BladeUser user) {
		User detail = userService.getById(user.getUserId());
		return R.data(UserWrapper.build().entityVO(detail));
	}

	/**
	 * User list
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "account", description = "Account Name", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "realName", description = "Name", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 3)
	@Operation(summary = "List", description = "Pass in account and realName")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<IPage<UserVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> user, Query query, BladeUser bladeUser) {
		QueryWrapper<User> queryWrapper = Condition.getQueryWrapper(user, User.class);
		IPage<User> pages = userService.page(Condition.getPage(query), (!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ? queryWrapper.lambda().eq(User::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(UserWrapper.build().pageVO(pages));
	}

	/**
	 * Add or modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add or modify", description = "Pass in User")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R submit(@Valid @RequestBody User user) {
		return R.status(userService.submit(user));
	}

	/**
	 * Modify
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in User")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R update(@Valid @RequestBody User user) {
		return R.status(userService.updateById(user));
	}

	/**
	 * Delete
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Delete", description = "Pass in foundation and")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R remove(@RequestParam String ids) {
		return R.status(userService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * Set menu permissions
	 *
	 * @param userIds
	 * @param roleIds
	 * @return
	 */
	@PostMapping("/grant")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Permission Settings", description = "Pass in roleId collection and menuId collection")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R grant(@Parameter(description = "userId collection", required = true) @RequestParam String userIds,
				   @Parameter(description = "roleId collection", required = true) @RequestParam String roleIds) {
		boolean temp = userService.grant(userIds, roleIds);
		return R.status(temp);
	}

	@PostMapping("/reset-password")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Initialize password", description = "Pass in userId collection")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R resetPassword(@Parameter(description = "userId collection", required = true) @RequestParam String userIds) {
		boolean temp = userService.resetPassword(userIds);
		return R.status(temp);
	}

	/**
	 * Modify password
	 *
	 * @param oldPassword
	 * @param newPassword
	 * @param newPassword1
	 * @return
	 */
	@PostMapping("/update-password")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "Modify password", description = "Pass in password")
	public R updatePassword(BladeUser user, @Parameter(description = "Old password", required = true) @RequestParam String oldPassword,
							@Parameter(description = "New password", required = true) @RequestParam String newPassword,
							@Parameter(description = "New password", required = true) @RequestParam String newPassword1) {
		boolean temp = userService.updatePassword(user.getUserId(), oldPassword, newPassword, newPassword1);
		return R.status(temp);
	}

	/**
	 * User list
	 *
	 * @param user
	 * @return
	 */
	@GetMapping("/user-list")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "User list", description = "Pass in user")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<List<UserVO>> userList(User user) {
		List<User> list = userService.list(Condition.getQueryWrapper(user));
		return R.data(UserWrapper.build().listVO(list));
	}


	/**
	 * Import user
	 */
	@PostMapping("import-user")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "Import user", description = "Pass in excel")
	public R importUser(MultipartFile file, Integer isCovered) {
		String filename = file.getOriginalFilename();
		if (StringUtil.isBlank(filename)) {
			throw new RuntimeException("Please upload files!");
		}
		if ((!StringUtil.endsWithIgnoreCase(filename, ".xls") && !StringUtil.endsWithIgnoreCase(filename, ".xlsx"))) {
			throw new RuntimeException("Please upload the correct excel file!");
		}
		InputStream inputStream;
		try {
			UserImportListener importListener = new UserImportListener(userService);
			inputStream = new BufferedInputStream(file.getInputStream());
			ExcelReaderBuilder builder = FastExcel.read(inputStream, UserExcel.class, importListener);
			builder.doReadAll();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return R.success("Operation successful");
	}

	/**
	 * Export user
	 */
	@SneakyThrows
	@GetMapping("export-user")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "Export user", description = "Pass in user")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public void exportUser(@Parameter(hidden = true) @RequestParam Map<String, Object> user, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<User> queryWrapper = Condition.getQueryWrapper(user, User.class);
		if (!SecureUtil.isAdministrator()) {
			queryWrapper.lambda().eq(User::getTenantId, bladeUser.getTenantId());
		}
		queryWrapper.lambda().eq(User::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<UserExcel> list = userService.exportUser(queryWrapper);
		response.setContentType("application/vnd.ms-excel");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String fileName = URLEncoder.encode("User data export", StandardCharsets.UTF_8);
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		FastExcel.write(response.getOutputStream(), UserExcel.class).sheet("User data table").doWrite(list);
	}

	/**
	 * Export template
	 */
	@SneakyThrows
	@GetMapping("export-template")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "Export template")
	public void exportUser(HttpServletResponse response) {
		List<UserExcel> list = new ArrayList<>();
		response.setContentType("application/vnd.ms-excel");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String fileName = URLEncoder.encode("User data template", StandardCharsets.UTF_8);
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		FastExcel.write(response.getOutputStream(), UserExcel.class).sheet("User data table").doWrite(list);
	}

	/**
	 * Third-party registered user
	 */
	@PostMapping("/register-guest")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "Third-party registered user", description = "Pass in user")
	public R registerGuest(User user, Long oauthId) {
		return R.status(userService.registerGuest(user, oauthId));
	}

	/**
	 * User unlock
	 */
	@PostMapping("/unlock")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "Account unlock")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R unlock(String userIds) {
		if (StringUtil.isBlank(userIds)) {
			return R.fail("Please select at least one user");
		}
		List<User> userList = userService.list(Wrappers.<User>lambdaQuery().in(User::getId, Func.toLongList(userIds)));
		userList.forEach(user -> bladeRedis.del(CacheNames.tenantKey(user.getTenantId(), CacheNames.USER_FAIL_KEY, user.getAccount())));
		return R.success("Operation successful");
	}

	/**
	 * Modify basic information
	 */
	@PostMapping("/update-info")
	@ApiOperationSupport(order = 17)
	@Operation(summary = "Modify basic information", description = "Pass in User")
	public R updateInfo(@RequestBody User user) {
		return R.status(userService.updateUserInfo(user));
	}

}
