/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.List;

/**
 * user layer
 *
 * @author Lion Li
 */
public interface ISysUserService extends IService<SysUser> {


    TableDataInfo<SysUser> selectPageUserList(SysUser user, PageQuery pageQuery);

    /**
     * Query user list
     *
     * @param user userinfo
     * @return userinfocollectioninfo
     */
    List<SysUser> selectUserList(SysUser user);

    /**
     * Query already userrole list
     *
     * @param user userinfo
     * @return userinfocollectioninfo
     */
    TableDataInfo<SysUser> selectAllocatedList(SysUser user, PageQuery pageQuery);

    /**
     * Query not userrole list
     *
     * @param user userinfo
     * @return userinfocollectioninfo
     */
    TableDataInfo<SysUser> selectUnallocatedList(SysUser user, PageQuery pageQuery);

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
     * user IDQuery user
     *
     * @param userId user ID
     * @return userobjectinfo
     */
    SysUser selectUserById(String userId);

    /**
     * user IDQuery user role
     *
     * @param userName user
     * @return
     */
    String selectUserRoleGroup(String userName);

    /**
     * user IDQuery user
     *
     * @param userName user
     * @return
     */
    String selectUserPostGroup(String userName);

    /**
     * Validate usernamewhether
     *
     * @param user userinfo
     * @return
     */
    boolean checkUserNameUnique(SysUser user);

    /**
     * Validate whether
     *
     * @param user userinfo
     * @return
     */
    boolean checkPhoneUnique(SysUser user);

    /**
     * Validate emailwhether
     *
     * @param user userinfo
     * @return
     */
    boolean checkEmailUnique(SysUser user);

    /**
     * Validate userwhether operation
     *
     * @param user userinfo
     */
    void checkUserAllowed(SysUser user);

    /**
     * Validate userwhether data
     *
     * @param userId user ID
     */
    void checkUserDataScope(String userId);

    /**
     * Add userinfo
     *
     * @param user userinfo
     * @return
     */
    int insertUser(SysUser user);

    /**
     * userinfo
     *
     * @param user userinfo
     * @return
     */
    boolean registerUser(SysUser user);

    /**
     * Update userinfo
     *
     * @param user userinfo
     * @return
     */
    int updateUser(SysUser user);

    /**
     * user role
     *
     * @param userId user ID
     * @param roleIds role
     */
    void insertUserAuth(String userId, String[] roleIds);

    /**
     * Update user
     *
     * @param user userinfo
     * @return
     */
    int updateUserStatus(SysUser user);

    /**
     * Update user info
     *
     * @param user userinfo
     * @return
     */
    int updateUserProfile(SysUser user);

    /**
     * Update user
     *
     * @param userName user
     * @param avatar
     * @return
     */
    boolean updateUserAvatar(String userName, String avatar);

    /**
     * user
     *
     * @param user userinfo
     * @return
     */
    int resetPwd(SysUser user);

    /**
     * user
     *
     * @param userName user
     * @param password
     * @return
     */
    int resetUserPwd(String userName, String password);

    /**
     * user IDDelete user
     *
     * @param userId user ID
     * @return
     */
    int deleteUserById(String userId);

    /**
     * Batch delete userinfo
     *
     * @param userIds need to Delete user ID
     * @return
     */
    int deleteUserByIds(String[] userIds);

    /**
     * useruuidQuery user
     *
     * @param UserId useruuID
     * @return userobjectinfo
     */
    SysUser selectUserByUserId(String UserId);


    /**
     * user IDQuery user
     *
     * @param UserIds useruuID
     * @return userobjectinfo
     */
    List<SysUser> selectUserByUserIds( List<String > UserIds);

    /**
     * Get leaderinfo
     *
     */
    List<SysUser> getLeaders(String userId);
    List<SysUser> getLeadersSuperior(String userId);

//    List<SysUser> getLeaders(String userId);

}
