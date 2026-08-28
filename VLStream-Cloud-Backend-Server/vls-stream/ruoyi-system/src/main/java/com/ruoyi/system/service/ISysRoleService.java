/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysUserRoleView;

import java.util.List;
import java.util.Set;

/**
 * role layer
 *
 * @author Lion Li
 */
public interface ISysRoleService {


    TableDataInfo<SysRole> selectPageRoleList(SysRole role, PageQuery pageQuery);

    /**
     * Query roledata
     *
     * @param role roleinfo
     * @return roledataset info
     */
    List<SysRole> selectRoleList(SysRole role);

    /**
     * user IDQuery role list
     *
     * @param userId user ID
     * @return role
     */
    List<SysRole> selectRolesByUserId(String userId);

    /**
     * user IDQuery role
     *
     * @param userId user ID
     * @return
     */
    Set<String> selectRolePermissionByUserId(String userId);

    /**
     * Query all role
     *
     * @return role
     */
    List<SysRole> selectRoleAll();

    /**
     * user IDGet role
     *
     * @param userId user ID
     * @return in role ID
     */
    List<Long> selectRoleListByUserId(String userId);

    /**
     * role IDQuery role
     *
     * @param roleId role ID
     * @return roleobjectinfo
     */
    SysUserRoleView selectRoleById(String roleId);

    /**
     * user ID and role IDQuery role ( )
     *
     * @param userId user ID ( to null)
     * @param roleId role ID ( to null)
     * @return roleobjectinfo
     */
    SysUserRoleView selectRoleByCondition(String userId, String roleId);

    /**
     * Validate role namewhether
     *
     * @param role roleinfo
     * @return
     */
    boolean checkRoleNameUnique(SysRole role);

    /**
     * Validate role whether
     *
     * @param role roleinfo
     * @return
     */
    boolean checkRoleKeyUnique(SysRole role);

    /**
     * Validate rolewhether operation
     *
     * @param role roleinfo
     */
    void checkRoleAllowed(SysRole role);

    /**
     * Validate rolewhether data
     *
     * @param roleId roleid
     */
    void checkRoleDataScope(String roleId);

    /**
     * role IDQuery role
     *
     * @param roleId role ID
     * @return
     */
    long countUserRoleByRoleId(String  roleId);

    /**
     * Add roleinfo
     *
     * @param role roleinfo
     * @return
     */
    int insertRole(SysRole role);

    /**
     * Update roleinfo
     *
     * @param role roleinfo
     * @return
     */
    int updateRole(SysRole role);

    /**
     * Update role
     *
     * @param role roleinfo
     * @return
     */
    int updateRoleStatus(SysRole role);

    /**
     * Update data info
     *
     * @param role roleinfo
     * @return
     */
    int authDataScope(SysRole role);

    /**
     * role IDDelete role
     *
     * @param roleId role ID
     * @return
     */
    int deleteRoleById(String roleId);

    /**
     * Batch delete roleinfo
     *
     * @param roleIds need to Delete role ID
     * @return
     */
    int deleteRoleByIds(String[] roleIds);

    /**
     * userrole
     *
     * @param userRole user and role info
     * @return
     */
    int deleteAuthUser(SysUserRoleView userRole);

    /**
     * userrole
     *
     * @param roleId role ID
     * @param userIds need to userdataID
     * @return
     */
    int deleteAuthUsers(String roleId, String[] userIds);

    /**
     * userrole
     *
     * @param roleId role ID
     * @param userIds need to Delete userdataID
     * @return
     */
    int insertAuthUsers(String roleId, String[] userIds);

    void cleanOnlineUserByRole(String roleId);
}
