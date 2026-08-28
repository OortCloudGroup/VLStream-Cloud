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
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * user data layer
 *
 * @author Lion Li
 */
public interface SysUserMapper extends BaseMapperPlus<SysUserMapper, SysUser, SysUser> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    Page<SysUser> selectPageUserList(@Param("page") Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * Query user list
     *
     * @param queryWrapper Query
     * @return userinfocollectioninfo
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    List<SysUser> selectUserList(@Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * Query already userrole list
     *
     * @param queryWrapper Query
     * @return userinfocollectioninfo
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    Page<SysUser> selectAllocatedList(@Param("page") Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * Query not userrole list
     *
     * @param queryWrapper Query
     * @return userinfocollectioninfo
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    Page<SysUser> selectUnallocatedList(@Param("page") Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * user Query user
     *
     * @param userName user
     * @return userobjectinfo
     */
    SysUser selectUserByUserName(String userName);

    /**
     * Query user
     *
     * @param phonenumber
     * @return userobjectinfo
     */
    SysUser selectUserByPhonenumber(String phonenumber);

    /**
     * Query user
     *
     * @param email
     * @return userobjectinfo
     */
    SysUser selectUserByEmail(String email);

    /**
     * user IDQuery user
     *
     * @param userId user ID
     * @return userobjectinfo
     */
    SysUser selectUserById(String userId);

    @Select("SELECT oort_uuid FROM sys_user")
    List<String> selectOortUuidList();

    /**
     * useruuidQuery user
     *
     * @param UserId useruuID
     * @return userobjectinfo
     */
    SysUser selectUserByUserId(@Param("UserId") String UserId);

    /**
     * user IDQuery user
     *
     * @param UserIds useruuID
     * @return userobjectinfo
     */
    List<SysUser> selectUserByUserIds(@Param("UserIds") List<String > UserIds);

    Date selectLatestUpdateTime();

    /**
     * parentIddepartment ID
     * @param parentId
     * @return
     */
    List<SysUser> selectLeadersByDeptId(String parentId);
}
