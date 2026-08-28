/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SysUserRoleView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * user and role data layer
 *
 * @author Lion Li
 */
public interface SysUserRoleViewMapper extends BaseMapperPlus<SysUserRoleViewMapper, SysUserRoleView, SysUserRoleView> {

    List<String> selectUserIdsByRoleId(Long roleId);

    /**
     * user ID and role IDQuery role ( )
     *
     * @param userId user ID ( to null)
     * @param roleId role ID ( to null)
     * @return roleobjectinfo
     */
    SysUserRoleView selectByCondition(@Param("userId") String userId, @Param("roleId")String roleId);

}
