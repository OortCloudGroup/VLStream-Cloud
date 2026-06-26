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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.system.entity.Role;
import org.springblade.modules.system.vo.RoleVO;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Service Class
 *
 * @author Chill
 */
public interface IRoleService extends IService<Role> {

	/**
	 * Custom paging
	 *
	 * @param page
	 * @param role
	 * @return
	 */
	IPage<RoleVO> selectRolePage(IPage<RoleVO> page, RoleVO role);

	/**
	 * Tree Structure
	 *
	 * @param tenantId
	 * @return
	 */
	List<RoleVO> tree(String tenantId);

	/**
	 * Permission Configuration
	 *
	 * @param roleIds      role ID set
	 * @param menuIds      menu ID set
	 * @param dataScopeIds data scope ID set
	 * @param apiScopeIds  API permission ID set
	 * @return whether successful
	 */
	boolean grant(@NotEmpty List<Long> roleIds, List<Long> menuIds, List<Long> dataScopeIds, List<Long> apiScopeIds);

	/**
	 * Get role ID
	 *
	 * @param tenantId
	 * @param roleNames
	 * @return
	 */
	String getRoleIds(String tenantId, String roleNames);

	/**
	 * Get role name
	 *
	 * @param roleIds
	 * @return
	 */
	List<String> getRoleNames(String roleIds);

}
