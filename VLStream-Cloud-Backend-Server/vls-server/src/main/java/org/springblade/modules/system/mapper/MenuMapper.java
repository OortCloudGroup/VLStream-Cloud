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

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.dto.MenuDTO;
import org.springblade.modules.system.entity.Menu;
import org.springblade.modules.system.vo.MenuVO;

import java.util.List;
import java.util.Map;

/**
 * Mapper interface
 *
 * @author Chill
 */
public interface MenuMapper extends BaseMapper<Menu> {

	/**
	 * Custom paging
	 *
	 * @param page
	 * @param menu
	 * @return
	 */
	List<MenuVO> selectMenuPage(IPage page, MenuVO menu);

	/**
	 * Lazy loaded menu list
	 *
	 * @param parentId
	 * @param param
	 * @return
	 */
	List<MenuVO> lazyMenuList(Long parentId, Map<String, Object> param);

	/**
	 * Tree Structure
	 *
	 * @return
	 */
	List<MenuVO> tree();

	/**
	 * Authorization tree structure
	 *
	 * @return
	 */
	List<MenuVO> grantTree();

	/**
	 * Authorization tree structure
	 *
	 * @return
	 */
	List<MenuVO> grantTreeByRole(List<Long> roleId);

	/**
	 * Data permission authorization tree structure
	 *
	 * @return
	 */
	List<MenuVO> grantDataScopeTree();

	/**
	 * Data permission authorization tree structure
	 *
	 * @param roleId
	 * @return
	 */
	List<MenuVO> grantDataScopeTreeByRole(List<Long> roleId);

	/**
	 * Interface permission authorization tree structure
	 *
	 * @return
	 */
	List<MenuVO> grantApiScopeTree();

	/**
	 * Interface permission authorization tree structure
	 *
	 * @param roleId
	 * @return
	 */
	List<MenuVO> grantApiScopeTreeByRole(List<Long> roleId);

	/**
	 * Top menu tree structure
	 *
	 * @return
	 */
	List<MenuVO> grantTopTree();

	/**
	 * Top menu tree structure
	 *
	 * @param roleId
	 * @return
	 */
	List<MenuVO> grantTopTreeByRole(List<Long> roleId);

	/**
	 * All menus
	 *
	 * @return
	 */
	List<Menu> allMenu();

	/**
	 * Permission Configuration Menu
	 *
	 * @param roleId
	 * @return
	 */
	List<Menu> roleMenu(List<Long> roleId);

	/**
	 * Menu tree structure
	 *
	 * @param roleId
	 * @return
	 */
	List<Menu> routes(List<Long> roleId);

	/**
	 * Button tree structure
	 *
	 * @param roleId
	 * @return
	 */
	List<Menu> buttons(List<Long> roleId);

	/**
	 * Get configured role permissions
	 * @param roleIds
	 * @return
	 */
	List<MenuDTO> authRoutes(List<Long> roleIds);

	/**
	 * Get role menus by role ID
	 *
	 * @param roleId
	 * @return
	 */
	List<Menu> roleMenuByRoleId(List<Long> roleId);

	/**
	 * Get menus by top menu ID
	 *
	 * @param topMenuId
	 * @return
	 */
	List<Menu> roleMenuByTopMenuId(Long topMenuId);
}
