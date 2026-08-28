/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.system.domain.vo.RouterVo;

import java.util.List;
import java.util.Set;

/**
 * menu layer
 *
 * @author Lion Li
 */
public interface ISysMenuService {

    /**
     * userQuery menu list
     *
     * @param userId user ID
     * @return menu
     */
    List<SysMenu> selectMenuList(String userId);

    /**
     * userQuery menu list
     *
     * @param menu menuinfo
     * @param userId user ID
     * @return menu
     */
    List<SysMenu> selectMenuList(SysMenu menu, String userId);

    /**
     * user IDQuery
     *
     * @param userId user ID
     * @return
     */
    Set<String> selectMenuPermsByUserId(String userId);

    /**
     * role IDQuery
     *
     * @param roleId role ID
     * @return
     */
    Set<String> selectMenuPermsByRoleId(Long roleId);

    /**
     * user IDQuery menu info
     *
     * @param userId user ID
     * @return menu
     */
    List<SysMenu> selectMenuTreeByUserId(String userId);

    /**
     * role IDQuery menu info
     *
     * @param roleId role ID
     * @return in menu
     */
    List<Long> selectMenuListByRoleId(Long roleId);

    /**
     * Build before need to menu
     *
     * @param menus menu
     * @return
     */
    List<RouterVo> buildMenus(List<SysMenu> menus);

    /**
     * Build before need to
     *
     * @param menus menu
     * @return
     */
    List<Tree<String >> buildMenuTreeSelect(List<SysMenu> menus);

    /**
     * menu IDQuery info
     *
     * @param menuId menu ID
     * @return menuinfo
     */
    SysMenu selectMenuById(Long menuId);

    /**
     * whether in menu sub node
     *
     * @param menuId menu ID
     * @return true in false in
     */
    boolean hasChildByMenuId(Long menuId);

    /**
     * Query menuwhether in role
     *
     * @param menuId menu ID
     * @return true in false in
     */
    boolean checkMenuExistRole(Long menuId);

    /**
     * Add menuinfo
     *
     * @param menu menuinfo
     * @return
     */
    int insertMenu(SysMenu menu);

    /**
     * Update menuinfo
     *
     * @param menu menuinfo
     * @return
     */
    int updateMenu(SysMenu menu);

    /**
     * Delete menu info
     *
     * @param menuId menu ID
     * @return
     */
    int deleteMenuById(Long menuId);

    /**
     * Validate menu namewhether
     *
     * @param menu menuinfo
     * @return
     */
    boolean checkMenuNameUnique(SysMenu menu);
}
