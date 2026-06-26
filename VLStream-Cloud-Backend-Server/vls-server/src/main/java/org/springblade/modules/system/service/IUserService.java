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
package org.springblade.modules.system.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.entity.UserOauth;
import org.springblade.modules.system.excel.UserExcel;

import java.util.List;

/**
 * Service Class
 *
 * @author Chill
 */
public interface IUserService extends BaseService<User> {

	/**
	 * Add or modify user
	 * @param user
	 * @return
	 */
	boolean submit(User user);

	/**
	 * Modify user basic info
	 *
	 * @param user
	 * @return
	 */
	boolean updateUserInfo(User user);

	/**
	 * Custom paging
	 *
	 * @param page
	 * @param user
	 * @return
	 */
	IPage<User> selectUserPage(IPage<User> page, User user);

	/**
	 * User information
	 *
	 * @param userId
	 * @return
	 */
	UserInfo userInfo(Long userId);

	/**
	 * User information
	 *
	 * @param tenantId
	 * @param account
	 * @param password
	 * @return
	 */
	UserInfo userInfo(String tenantId, String account, String password);

	/**
	 * User information
	 *
	 * @param userOauth
	 * @return
	 */
	UserInfo userInfo(UserOauth userOauth);

	/**
	 * Assign roles to users
	 *
	 * @param userIds
	 * @param roleIds
	 * @return
	 */
	boolean grant(String userIds, String roleIds);

	/**
	 * Initialize password
	 *
	 * @param userIds
	 * @return
	 */
	boolean resetPassword(String userIds);

	/**
	 * Modify password
	 *
	 * @param userId
	 * @param oldPassword
	 * @param newPassword
	 * @param newPassword1
	 * @return
	 */
	boolean updatePassword(Long userId, String oldPassword, String newPassword, String newPassword1);

	/**
	 * Get role name
	 *
	 * @param roleIds
	 * @return
	 */
	List<String> getRoleName(String roleIds);

	/**
	 * Get department name
	 *
	 * @param deptIds
	 * @return
	 */
	List<String> getDeptName(String deptIds);

	/**
	 * Import user data
	 *
	 * @param data
	 * @return
	 */
	void importUser(List<UserExcel> data);

	/**
	 * Get export user data
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<UserExcel> exportUser(Wrapper<User> queryWrapper);

	/**
	 * Registered users
	 *
	 * @param user
	 * @param oauthId
	 * @return
	 */
	boolean registerGuest(User user, Long oauthId);
}
