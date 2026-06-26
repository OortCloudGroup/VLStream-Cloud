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
package org.springblade.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.excel.UserExcel;

import java.util.List;

/**
 * Mapper interface
 *
 * @author Chill
 */
public interface UserMapper extends BaseMapper<User> {

	/**
	 * Custom paging
	 *
	 * @param page
	 * @param user
	 * @return
	 */
	List<User> selectUserPage(IPage page, User user);

	/**
	 * Get user
	 *
	 * @param tenantId
	 * @param account
	 * @param password
	 * @return
	 */
	User getUser(String tenantId, String account, String password);

	/**
	 * Get role name
	 *
	 * @param ids
	 * @return
	 */
	List<String> getRoleName(String[] ids);

	/**
	 * Get role alias
	 *
	 * @param ids
	 * @return
	 */
	List<String> getRoleAlias(String[] ids);

	/**
	 * Get department name
	 *
	 * @param ids
	 * @return
	 */
	List<String> getDeptName(String[] ids);

	/**
	 * Get export user data
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<UserExcel> exportUser(@Param("ew") Wrapper<User> queryWrapper);

}
