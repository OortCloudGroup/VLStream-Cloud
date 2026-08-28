/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysRoleDept;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.domain.SysUserRoleView;
import com.ruoyi.system.mapper.SysRoleDeptMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysUserRoleViewMapper;
import com.ruoyi.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * role layer Process
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper baseMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleViewMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    @Override
    public TableDataInfo<SysRole> selectPageRoleList(SysRole role, PageQuery pageQuery) {
        Page<SysRole> page = baseMapper.selectPageRoleList(pageQuery.build(), this.buildQueryWrapper(role));
        return TableDataInfo.build(page);
    }

    /**
     * Query roledata
     *
     * @param role roleinfo
     * @return roledataset info
     */
    @Override
    public List<SysRole> selectRoleList(SysRole role) {
        return baseMapper.selectRoleList(this.buildQueryWrapper(role));
    }

    private Wrapper<SysRole> buildQueryWrapper(SysRole role) {
        Map<String, Object> params = role.getParams();
        QueryWrapper<SysRole> wrapper = Wrappers.query();
        wrapper.eq("r.del_flag", UserConstants.ROLE_NORMAL)
            .eq(ObjectUtil.isNotNull(role.getRoleId()), "r.role_id", role.getRoleId())
            .like(StringUtils.isNotBlank(role.getRoleName()), "r.role_name", role.getRoleName())
            .eq(StringUtils.isNotBlank(role.getStatus()), "r.status", role.getStatus())
            .like(StringUtils.isNotBlank(role.getRoleKey()), "r.role_key", role.getRoleKey())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "r.create_time", params.get("beginTime"), params.get("endTime"))
            .orderByAsc("r.role_sort").orderByAsc("r.create_time");
        return wrapper;
    }

    /**
     * user IDQuery role
     *
     * @param userId user ID
     * @return role
     */
    @Override
    public List<SysRole> selectRolesByUserId(String userId) {
        List<SysRole> userRoles = baseMapper.selectRolePermissionByUserId(userId);
        List<SysRole> roles = selectRoleAll();
        for (SysRole role : roles) {
            for (SysRole userRole : userRoles) {
                if (role.getRoleId().equals(userRole.getRoleId())) {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * user IDQuery
     *
     * @param userId user ID
     * @return
     */
    @Override
    public Set<String> selectRolePermissionByUserId(String userId) {
        List<SysRole> perms = baseMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms) {
            if (ObjectUtil.isNotNull(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.getRoleKey().trim()));
            }
        }
        return permsSet;
    }

    /**
     * Query all role
     *
     * @return role
     */
    @Override
    public List<SysRole> selectRoleAll() {
        return this.selectRoleList(new SysRole());
    }

    /**
     * user IDGet role
     *
     * @param userId user ID
     * @return in role ID
     */
    @Override
    public List<Long> selectRoleListByUserId(String userId) {
        return baseMapper.selectRoleListByUserId(userId);
    }

    /**
     * role IDQuery role
     *
     * @param roleId role ID
     * @return roleobjectinfo
     */
    @Override
    public SysUserRoleView selectRoleById(String roleId) {
        return userRoleMapper.selectById(roleId);
    }

    /**
     * user ID and role IDQuery role ( )
     *
     * @param userId user ID ( to null)
     * @param roleId role ID ( to null)
     * @return roleobjectinfo
     */
    @Override
    public SysUserRoleView selectRoleByCondition(String userId, String roleId) {
        return userRoleMapper.selectByCondition(userId, roleId);
    }


    /**
     * Validate role namewhether
     *
     * @param role roleinfo
     * @return
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleName, role.getRoleName())
            .ne(ObjectUtil.isNotNull(role.getRoleId()), SysRole::getRoleId, role.getRoleId()));
        return !exist;
    }

    /**
     * Validate role whether
     *
     * @param role roleinfo
     * @return
     */
    @Override
    public boolean checkRoleKeyUnique(SysRole role) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleKey, role.getRoleKey())
            .ne(ObjectUtil.isNotNull(role.getRoleId()), SysRole::getRoleId, role.getRoleId()));
        return !exist;
    }

    /**
     * Validate rolewhether operation
     *
     * @param role roleinfo
     */
    @Override
    public void checkRoleAllowed(SysRole role) {
        if (ObjectUtil.isNotNull(role.getRoleId()) && role.isAdmin()) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
    }

    /**
     * Validate rolewhether data
     *
     * @param roleId roleid
     */
    @Override
    public void checkRoleDataScope(String roleId) {
        if (!LoginHelper.isAdmin()) {
            SysRole role = new SysRole();
            role.setRoleId(roleId);
            List<SysRole> roles = this.selectRoleList(role);
            if (CollUtil.isEmpty(roles)) {
                throw new ServiceException("没有权限访问角色数据！");
            }
        }
    }

    /**
     * role IDQuery role
     *
     * @param roleId role ID
     * @return
     */
    @Override
    public long countUserRoleByRoleId(String roleId) {
        return userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleView>().eq(SysUserRoleView::getRoleId, roleId));
    }

    /**
     * Add roleinfo
     *
     * @param role roleinfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRole(SysRole role) {
        // Add roleinfo
        baseMapper.insert(role);
        return insertRoleMenu(role);
    }

    /**
     * Update roleinfo
     *
     * @param role roleinfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(SysRole role) {
        // Update roleinfo
        baseMapper.updateById(role);
        // Delete role and menu
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getRoleId()));
        return insertRoleMenu(role);
    }

    /**
     * Update role
     *
     * @param role roleinfo
     * @return
     */
    @Override
    public int updateRoleStatus(SysRole role) {
        return baseMapper.updateById(role);
    }

    /**
     * Update data info
     *
     * @param role roleinfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int authDataScope(SysRole role) {
        // Update roleinfo
        baseMapper.updateById(role);
        // Delete role and department
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        // Add role and departmentinfo (data )
        return insertRoleDept(role);
    }

    /**
     * Add rolemenuinfo
     *
     * @param role roleobject
     */
    public int insertRoleMenu(SysRole role) {
        int rows = 1;
        // Add user and role
        List<SysRoleMenu> list = new ArrayList<SysRoleMenu>();
        for (Long menuId : role.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        if (list.size() > 0) {
            rows = roleMenuMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * Add roledepartmentinfo(data )
     *
     * @param role roleobject
     */
    public int insertRoleDept(SysRole role) {
        int rows = 1;
        // Add role and department (data )
        List<SysRoleDept> list = new ArrayList<SysRoleDept>();
        for (String deptId : role.getDeptIds()) {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        if (list.size() > 0) {
            rows = roleDeptMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * role IDDelete role
     *
     * @param roleId role ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleById(String  roleId) {
        // Delete role and menu
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        // Delete role and department
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        return baseMapper.deleteById(roleId);
    }

    /**
     * Batch delete roleinfo
     *
     * @param roleIds need to Delete role ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIds(String [] roleIds) {
        for (String roleId : roleIds) {
            checkRoleAllowed(new SysRole(roleId));
            checkRoleDataScope(roleId);
            SysUserRoleView role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0) {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        List<String> ids = Arrays.asList(roleIds);
        // Delete role and menu
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, ids));
        // Delete role and department
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, ids));
        return baseMapper.deleteBatchIds(ids);
    }

    /**
     * userrole
     *
     * @param userRole user and role info
     * @return
     */
    @Override
    public int deleteAuthUser(SysUserRoleView userRole) {
        int rows = userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>()
            .eq(SysUserRoleView::getRoleId, userRole.getRoleId())
            .eq(SysUserRoleView::getUserId, userRole.getUserId()));
        if (rows > 0) {
            cleanOnlineUserByRole(userRole.getRoleId());
        }
        return rows;
    }

    /**
     * userrole
     *
     * @param roleId role ID
     * @param userIds need to userdataID
     * @return
     */
    @Override
    public int deleteAuthUsers(String roleId, String[] userIds) {
        int rows = userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>()
            .eq(SysUserRoleView::getRoleId, roleId)
            .in(SysUserRoleView::getUserId, Arrays.asList(userIds)));
        if (rows > 0) {
            cleanOnlineUserByRole(roleId);
        }
        return rows;
    }

    /**
     * userrole
     *
     * @param roleId role ID
     * @param userIds need to userdataID
     * @return
     */
    @Override
    public int insertAuthUsers(String roleId, String[] userIds) {
        // Add user and role
        int rows = 1;
        List<SysUserRoleView> list = StreamUtils.toList(Arrays.asList(userIds), userId -> {
            SysUserRoleView ur = new SysUserRoleView();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        });
        if (CollUtil.isNotEmpty(list)) {
            rows = userRoleMapper.insertBatch(list) ? list.size() : 0;
        }
        if (rows > 0) {
            cleanOnlineUserByRole(roleId);
        }
        return rows;
    }

    @Override
    public void cleanOnlineUserByRole(String  roleId) {
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // role in user will redis operation
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, ":");
            // if already
            if (StpUtil.stpLogic.getTokenActivityTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (loginUser.getRoles().stream().anyMatch(r -> r.getRoleId().equals(roleId))) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }
}
