/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * menu data layer
 *
 * @author Lion Li
 */
public interface SysMenuMapper extends BaseMapperPlus<SysMenuMapper, SysMenu, SysMenu> {

    /**
     * userall
     *
     * @return
     */
    List<String> selectMenuPerms();

    /**
     * userQuery menu list
     *
     * @param queryWrapper Query
     * @return menu
     */
    List<SysMenu> selectMenuListByUserId(@Param(Constants.WRAPPER) Wrapper<SysMenu> queryWrapper);

    /**
     * user IDQuery
     *
     * @param userId user ID
     * @return
     */
    List<String> selectMenuPermsByUserId(String userId);

    /**
     * role IDQuery
     *
     * @param roleId role ID
     * @return
     */
    List<String> selectMenuPermsByRoleId(Long roleId);

    /**
     * user IDQuery menu
     *
     * @return menu
     */
    default List<SysMenu> selectMenuTreeAll() {
        LambdaQueryWrapper<SysMenu> lqw = new LambdaQueryWrapper<SysMenu>()
            .in(SysMenu::getMenuType, UserConstants.TYPE_DIR, UserConstants.TYPE_MENU)
            .eq(SysMenu::getStatus, UserConstants.MENU_NORMAL)
            .orderByAsc(SysMenu::getParentId)
            .orderByAsc(SysMenu::getOrderNum);
        return this.selectList(lqw);
    }

    /**
     * user IDQuery menu
     *
     * @param userId user ID
     * @return menu
     */
    List<SysMenu> selectMenuTreeByUserId(String userId);

    /**
     * role IDQuery menu info
     *
     * @param roleId role ID
     * @param menuCheckStrictly menu item whether
     * @return in menu
     */
    List<Long> selectMenuListByRoleId(@Param("roleId") Long roleId, @Param("menuCheckStrictly") boolean menuCheckStrictly);

}
