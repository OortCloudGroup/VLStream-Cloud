/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
        return userRoleMapper.selectByCondition(null, roleId);
    }

    @Override
    public SysRole selectRoleDefinitionById(String roleId) {
        return baseMapper.selectById(roleId);
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
     * Built-in roles must never be deleted; batch deletion checks every id.
     */
    private SysRole checkRoleDeletable(String roleId) {
        if (StringUtils.isBlank(roleId)) {
            throw new ServiceException("角色ID不能为空", 400);
        }
        SysRole role = baseMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在或已删除，不允许删除", 404);
        }
        String roleKey = role.getRoleKey();
        if (role.isAdmin()
            || "1900000000000004001".equals(roleId)
            || "admin".equalsIgnoreCase(roleKey)
            || "tenant_admin".equalsIgnoreCase(roleKey)
            || "vls_member".equalsIgnoreCase(roleKey)) {
            throw new ServiceException("初始化角色不允许删除", 403);
        }
        return role;
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
        if (role.getRoleId() != null || isBuiltInKey(role.getRoleKey())) {
            throw new ServiceException("不能通过业务角色接口创建系统内置角色");
        }
        if (role.getMenuIds() != null && role.getMenuIds().length > 0) {
            throw new ServiceException("请创建角色后通过菜单授权接口分配权限");
        }
        LoginUser login = LoginHelper.getLoginUser();
        if (login == null || StringUtils.isBlank(login.getTenantId())) {
            throw new ServiceException("缺少可信租户上下文");
        }
        role.setTenantId(login.getTenantId());
        role.setDelFlag("0");
        role.setStatus("0");
        role.setDataScope("5");
        return baseMapper.insert(role);
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
        // Menu grants have their own scope and optimistic-concurrency checks.
        // A metadata edit must never replace them with an absent/empty form field.
        checkRoleAllowed(role);
        checkRoleDataScope(role.getRoleId());
        SysRole stored = baseMapper.selectById(role.getRoleId());
        if (stored == null) {
            throw new ServiceException("角色不存在或已删除");
        }
        boolean builtIn = isBuiltInKey(stored.getRoleKey());
        if (!builtIn && isBuiltInKey(role.getRoleKey())) {
            throw new ServiceException("不能将业务角色改为系统内置角色");
        }
        if (builtIn && (!Objects.equals(stored.getRoleKey(), role.getRoleKey())
            || !Objects.equals(stored.getRoleName(), role.getRoleName())
            || (role.getStatus() != null && !Objects.equals(stored.getStatus(), role.getStatus())))) {
            throw new ServiceException("初始化角色的名称、标识和状态不允许修改");
        }
        role.setTenantId(stored.getTenantId());
        role.setDataScope(stored.getDataScope());
        role.setMenuCheckStrictly(stored.getMenuCheckStrictly());
        role.setDeptCheckStrictly(stored.getDeptCheckStrictly());
        role.setDelFlag(null);
        return baseMapper.updateById(role);
    }

    private boolean isBuiltInKey(String key) {
        return key != null && ("admin".equalsIgnoreCase(key.trim())
            || "tenant_admin".equalsIgnoreCase(key.trim()) || "vls_member".equalsIgnoreCase(key.trim()));
    }

    /**
     * Update role
     *
     * @param role roleinfo
     * @return
     */
    @Override
    public int updateRoleStatus(SysRole role) {
        checkRoleAllowed(role);
        checkRoleDataScope(role.getRoleId());
        SysRole stored = baseMapper.selectById(role.getRoleId());
        if (stored == null || isBuiltInKey(stored.getRoleKey())) {
            throw new ServiceException("初始化角色的状态不允许修改");
        }
        if (!"0".equals(role.getStatus()) && !"1".equals(role.getStatus())) {
            throw new ServiceException("角色状态无效");
        }
        SysRole update = new SysRole(role.getRoleId());
        update.setStatus(role.getStatus());
        return baseMapper.updateById(update);
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
        checkRoleAllowed(role);
        checkRoleDataScope(role.getRoleId());
        SysRole stored = baseMapper.selectById(role.getRoleId());
        if (stored == null || role.getDataScope() == null || !role.getDataScope().matches("[1-5]")) {
            throw new ServiceException("角色或数据范围无效");
        }
        if ("2".equals(role.getDataScope())) {
            if (role.getDeptIds() == null) throw new ServiceException("部门列表不能为空");
            for (String deptId : role.getDeptIds()) {
                if (StringUtils.isBlank(deptId) || baseMapper.countAccessibleDepartment(deptId) != 1) {
                    throw new ServiceException("部门不存在或不在当前租户范围");
                }
            }
        }
        SysRole update = new SysRole(role.getRoleId());
        update.setDataScope(role.getDataScope());
        int rows = baseMapper.updateById(update);
        // Delete role and department
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        // Add role and departmentinfo (data )
        if (rows > 0 && "2".equals(role.getDataScope())) insertRoleDept(role);
        return rows;
    }

    /**
     * Add rolemenuinfo
     *
     * @param role roleobject
     */
    public int insertRoleMenu(SysRole role) {
        int rows = 1;
        if (role.getMenuIds() == null) {
            return rows;
        }
        // Add user and role
        List<SysRoleMenu> list = new ArrayList<SysRoleMenu>();
        for (Long menuId : role.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setTenantId(role.getTenantId());
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
        checkRoleDeletable(roleId);
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
            SysRole role = checkRoleDeletable(roleId);
            checkRoleDataScope(roleId);
            if (countUserRoleByRoleId(roleId) > 0) {
                throw new ServiceException(String.format("%1$s已分配,不能删除",
                    role == null ? roleId : role.getRoleName()), 409);
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
        validateAssignment(userRole.getRoleId(), new String[] {userRole.getUserId()}, false);
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
        validateAssignment(roleId, userIds, false);
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
        String tenant = validateAssignment(roleId, userIds, true);
        // Add user and role
        int rows = 1;
        List<SysUserRoleView> list = StreamUtils.toList(Arrays.asList(userIds), userId -> {
            SysUserRoleView ur = new SysUserRoleView();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setTenantId(tenant);
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

    private String validateAssignment(String roleId, String[] userIds, boolean granting) {
        checkRoleDataScope(roleId);
        LoginUser actor = LoginHelper.getLoginUser();
        SysRole role = baseMapper.selectById(roleId);
        if (actor == null || StringUtils.isBlank(actor.getTenantId()) || role == null
            || role.isAdmin() || !actor.getTenantId().equals(role.getTenantId())
            || (granting && !"0".equals(role.getStatus()))) {
            throw new ServiceException("角色不存在、停用或不在当前租户范围");
        }
        if (!LoginHelper.isAdmin(actor.getUserId())
            && ("tenant_admin".equalsIgnoreCase(role.getRoleKey())
                || !new HashSet<>(baseMapper.selectAssignedMenuIds(actor.getUserId()))
                    .containsAll(baseMapper.selectRoleMenuIds(roleId)))) {
            throw new ServiceException("不能管理超出本人授权范围的角色成员");
        }
        if (userIds == null || userIds.length == 0) {
            throw new ServiceException("请选择用户");
        }
        Set<String> seen = new HashSet<>();
        for (String userId : userIds) {
            if (StringUtils.isBlank(userId) || "1".equals(userId) || !seen.add(userId)
                || userRoleMapper.countTenantUser(userId, actor.getTenantId()) != 1) {
                throw new ServiceException("不能修改超级管理员、其他租户或不存在的用户");
            }
        }
        return actor.getTenantId();
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
            if (loginUser != null && loginUser.getRoles() != null
                && loginUser.getRoles().stream().anyMatch(r -> r.getRoleId().equals(roleId))) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }
}
