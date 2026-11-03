package com.ruoyi.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.RoleMenuGrantUpdate;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Changes menu grants without updating role metadata or overwriting grants that
 * the caller is unable to see and grant.
 */
@Service
@RequiredArgsConstructor
public class RoleMenuGrantService {
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;

    @Transactional(readOnly = true)
    public List<String> getMenuIds(String roleId) {
        checkedRole(roleId, false);
        return toStrings(readGrants(roleId));
    }

    @Transactional(readOnly = true)
    public List<String> getGrantableMenuIds(String roleId) {
        SysRole role = checkedRole(roleId, false);
        if ("1".equals(role.getRoleId()) && !currentUserIsAdmin()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Long id : new TreeSet<>(grantableIds(role))) {
            result.add(String.valueOf(id));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMenuIds(String roleId, RoleMenuGrantUpdate request) {
        SysRole role = checkedRole(roleId, true);
        if ("1".equals(role.getRoleId())) {
            // Only a current superuser may manage menu bindings of the
            // protected role. General role update APIs remain prohibited.
            if (!currentUserIsAdmin()) {
                throw new ServiceException("只有超级管理员可以调整超级管理员角色的菜单授权");
            }
        } else {
            roleService.checkRoleAllowed(role);
        }
        if (request == null) {
            throw new ServiceException("授权数据不能为空");
        }
        Set<Long> requested = parseIds(request.getMenuIds());
        Set<Long> expected = parseIds(request.getExpectedMenuIds());
        List<SysRoleMenu> current = readGrants(roleId);
        Set<Long> currentIds = toIds(current);
        if (!currentIds.equals(expected)) {
            throw new ServiceException("角色权限已变更，请刷新后重试");
        }

        Set<Long> grantable = grantableIds(role);
        if (!grantable.containsAll(requested)) {
            throw new ServiceException("不能分配超出本人授权范围的菜单");
        }
        Set<Long> existing = new HashSet<>(currentIds);
        for (SysRoleMenu grant : current) {
            Long menuId = grant.getMenuId();
            // Rows with a different or missing tenant are historical data outside
            // the caller's editable range. Preserve them exactly.
            if (grantable.contains(menuId) && !requested.contains(menuId)
                && Objects.equals(role.getTenantId(), grant.getTenantId())) {
                roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, roleId)
                    .eq(SysRoleMenu::getTenantId, role.getTenantId())
                    .eq(SysRoleMenu::getMenuId, menuId));
            }
        }
        for (Long menuId : requested) {
            if (!existing.contains(menuId)) {
                SysRoleMenu grant = new SysRoleMenu();
                grant.setRoleId(roleId);
                grant.setTenantId(role.getTenantId());
                grant.setMenuId(menuId);
                roleMenuMapper.insert(grant);
            }
        }
    }

    private SysRole checkedRole(String roleId, boolean lock) {
        if (roleId == null || !roleId.matches("[1-9][0-9]*")) {
            throw new ServiceException("角色编号无效");
        }
        roleService.checkRoleDataScope(roleId);
        LambdaQueryWrapper<SysRole> query = new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleId, roleId);
        if (lock) {
            query.last("FOR UPDATE");
        }
        SysRole role = roleMapper.selectOne(query);
        LoginUser login = currentLoginUser();
        if (role == null || login == null || role.getTenantId() == null
            || role.getTenantId().trim().isEmpty()
            || !Objects.equals(role.getTenantId(), login.getTenantId())) {
            throw new ServiceException("没有权限访问角色数据");
        }
        return role;
    }

    private List<SysRoleMenu> readGrants(String roleId) {
        return roleMenuMapper.selectAllForVerifiedRole(roleId);
    }

    private Set<Long> grantableIds(SysRole role) {
        String userId = currentUserId();
        List<SysMenu> menus = menuService.selectMenuList(userId);
        Set<Long> active = new HashSet<>();
        for (SysMenu menu : menus) {
            if ("0".equals(menu.getStatus())) {
                active.add(menu.getMenuId());
            }
        }
        // Use the same tree builder as /system/menu/treeselect. A grant whose
        // parent is not visible cannot be edited or accidentally deleted.
        Set<Long> grantable = new HashSet<>();
        List<Tree<String>> roots = menuService.buildMenuTreeSelect(menus);
        Deque<Tree<String>> queue = new ArrayDeque<>();
        if (roots != null) {
            queue.addAll(roots);
        }
        while (!queue.isEmpty()) {
            Tree<String> node = queue.removeFirst();
            Long id = Long.valueOf(node.getId());
            if (active.contains(id)) {
                grantable.add(id);
            }
            if (node.getChildren() != null) {
                queue.addAll(node.getChildren());
            }
        }
        // The presentation query includes bindings for disabled roles. It is not
        // sufficient as an authorization boundary, so intersect it with active
        // same-tenant role assignments for every non-superuser.
        if (!currentUserIsAdmin()) {
            grantable.retainAll(roleMenuMapper.selectGrantableMenuIds(userId, role.getTenantId()));
        }
        return grantable;
    }

    LoginUser currentLoginUser() {
        return LoginHelper.getLoginUser();
    }

    String currentUserId() {
        return LoginHelper.getUserId();
    }

    boolean currentUserIsAdmin() {
        return "1".equals(currentUserId()) || LoginHelper.isAdmin();
    }

    private Set<Long> parseIds(List<String> values) {
        if (values == null) {
            throw new ServiceException("菜单授权列表不能为空");
        }
        Set<Long> result = new HashSet<>();
        for (String value : values) {
            if (value == null || !value.matches("[1-9][0-9]*")) {
                throw new ServiceException("菜单编号无效");
            }
            try {
                if (!result.add(Long.valueOf(value))) {
                    throw new ServiceException("菜单编号重复");
                }
            } catch (NumberFormatException ex) {
                throw new ServiceException("菜单编号无效");
            }
        }
        return result;
    }

    private Set<Long> toIds(List<SysRoleMenu> grants) {
        Set<Long> result = new HashSet<>();
        for (SysRoleMenu grant : grants) {
            result.add(grant.getMenuId());
        }
        return result;
    }

    private List<String> toStrings(List<SysRoleMenu> grants) {
        List<String> result = new ArrayList<>();
        for (Long id : new TreeSet<>(toIds(grants))) {
            result.add(String.valueOf(id));
        }
        return result;
    }
}
