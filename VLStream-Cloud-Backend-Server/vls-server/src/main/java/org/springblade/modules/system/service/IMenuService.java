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
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.system.entity.Menu;
import org.springblade.modules.system.vo.MenuVO;

import java.util.List;
import java.util.Map;

/**
 * Service Class
 *
 * @author Chill
 */
public interface IMenuService extends IService<Menu> {

	/**
	 * Custom paging
	 *
	 * @param page
	 * @param menu
	 * @return
	 */
	IPage<MenuVO> selectMenuPage(IPage<MenuVO> page, MenuVO menu);

	/**
	 * Lazy loaded menu list
	 *
	 * @param parentId
	 * @param param
	 * @return
	 */
	List<MenuVO> lazyMenuList(Long parentId, Map<String, Object> param);

	/**
	 * Menu tree structure
	 *
	 * @param roleId
	 * @param topMenuId
	 * @return
	 */
	List<MenuVO> routes(String roleId, Long topMenuId);

	/**
	 * Button tree structure
	 *
	 * @param roleId
	 * @return
	 */
	List<MenuVO> buttons(String roleId);

	/**
	 * Tree Structure
	 *
	 * @return
	 */
	List<MenuVO> tree();

	/**
	 * Authorization tree structure
	 *
	 * @param user
	 * @return
	 */
	List<MenuVO> grantTree(BladeUser user);

	/**
	 * Data permission authorization tree structure
	 *
	 * @param user
	 * @return
	 */
	List<MenuVO> grantDataScopeTree(BladeUser user);

	/**
	 * Interface permission authorization tree structure
	 *
	 * @param user
	 * @return
	 */
	List<MenuVO> grantApiScopeTree(BladeUser user);

	/**
	 * Default selected node
	 *
	 * @param roleIds
	 * @return
	 */
	List<String> roleTreeKeys(String roleIds);

	/**
	 * Default selected node
	 *
	 * @param roleIds
	 * @return
	 */
	List<String> dataScopeTreeKeys(String roleIds);

	/**
	 * Interface permission default selected node
	 *
	 * @param roleIds
	 * @return
	 */
	List<String> apiScopeTreeKeys(String roleIds);

	/**
	 * Get configured role permissions
	 *
	 * @param user
	 * @return
	 */
	List<Kv> authRoutes(BladeUser user);

	/**
	 * Top menu authorization tree structure
	 *
	 * @param user
	 * @return
	 */
	List<MenuVO> grantTopTree(BladeUser user);

	/**
	 * Top menu default selected node
	 *
	 * @param topMenuIds
	 * @return
	 */
	List<String> topTreeKeys(String topMenuIds);

}
