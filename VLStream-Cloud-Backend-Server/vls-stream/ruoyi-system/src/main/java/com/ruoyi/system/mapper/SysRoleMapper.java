/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.DataColumn;
import com.ruoyi.common.annotation.DataPermission;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * role data layer
 *
 * @author Lion Li
 */
public interface SysRoleMapper extends BaseMapperPlus<SysRoleMapper, SysRole, SysRole> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id")
    })
    Page<SysRole> selectPageRoleList(@Param("page") Page<SysRole> page, @Param(Constants.WRAPPER) Wrapper<SysRole> queryWrapper);

    /**
     * Query roledata
     *
     * @param queryWrapper Query
     * @return roledataset info
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id")
    })
    List<SysRole> selectRoleList(@Param(Constants.WRAPPER) Wrapper<SysRole> queryWrapper);

    /**
     * user IDQuery role
     *
     * @param userId user ID
     * @return role
     */
    List<SysRole> selectRolePermissionByUserId(String userId);


    /**
     * user IDGet role
     *
     * @param userId user ID
     * @return in role ID
     */
    List<Long> selectRoleListByUserId(String userId);

    /**
     * user IDQuery role
     *
     * @param userName user
     * @return role
     */
    List<SysRole> selectRolesByUserName(String userName);

}
