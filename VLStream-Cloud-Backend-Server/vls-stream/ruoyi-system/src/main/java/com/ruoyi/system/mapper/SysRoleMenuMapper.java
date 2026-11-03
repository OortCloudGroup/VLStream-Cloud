/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * role and menu data layer
 *
 * @author Lion Li
 */
public interface SysRoleMenuMapper extends BaseMapperPlus<SysRoleMenuMapper, SysRoleMenu, SysRoleMenu> {

    // Caller must first verify the role belongs to the current tenant. Historical
    // rows may have a null or inconsistent tenant; include them for comparison
    // so a role edit never silently discards such grants.
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT role_id, tenant_id, menu_id FROM sys_role_menu WHERE role_id = #{roleId} ORDER BY menu_id")
    List<SysRoleMenu> selectAllForVerifiedRole(@Param("roleId") String roleId);

    // Explicit tenant checks are required because the caller uses this result
    // as an authorization boundary rather than a presentation-only menu tree.
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT rm.menu_id FROM sys_role_menu rm "
        + "JOIN sys_role r ON r.role_id = rm.role_id AND r.tenant_id = #{tenantId} "
        + "JOIN sys_user_role ur ON ur.role_id = r.role_id AND ur.tenant_id = #{tenantId} "
        + "JOIN sys_user u ON u.user_id = ur.user_id AND u.tenant_id = #{tenantId} "
        + "WHERE ur.user_id = #{userId} AND rm.tenant_id = #{tenantId} "
        + "AND r.status = '0' AND r.del_flag = '0' AND u.status = '0' AND u.del_flag = '0'")
    List<Long> selectGrantableMenuIds(@Param("userId") String userId, @Param("tenantId") String tenantId);
}
