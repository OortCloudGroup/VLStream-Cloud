/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysPost;
import com.ruoyi.system.mapper.SysPostMapper;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.compat.SingleTenant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BladeSystemDirectoryCompatController {

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;
    private final ISysMenuService menuService;
    private final SysPostMapper postMapper;

    public BladeSystemDirectoryCompatController(ISysUserService userService,
                                                ISysRoleService roleService,
                                                ISysDeptService deptService,
                                                ISysMenuService menuService,
                                                SysPostMapper postMapper) {
        this.userService = userService;
        this.roleService = roleService;
        this.deptService = deptService;
        this.menuService = menuService;
        this.postMapper = postMapper;
    }

    /**
     * Return users in SpringBlade page shape for the system user table.
     */
    @GetMapping("/blade-system/user/list")
    public BladeResult<BladePage<Map<String, Object>>> userList(HttpServletRequest request) {
        PageQuery pageQuery = buildPageQuery(request);
        TableDataInfo<SysUser> page = userService.selectPageUserList(buildUserFilter(request), pageQuery);
        return BladeResult.success(toPage(userMaps(page.getRows()), page.getTotal(), pageQuery));
    }

    /**
     * Return roles as a flat tree-ready array for role selectors and tables.
     */
    @GetMapping("/blade-system/role/list")
    public BladeResult<List<Map<String, Object>>> roleList() {
        return BladeResult.success(roleMaps(roleService.selectRoleList(new SysRole())));
    }

    /**
     * Return role options for the user management role selector.
     */
    @GetMapping("/blade-system/role/tree")
    public BladeResult<List<Map<String, Object>>> roleTree() {
        return BladeResult.success(roleMaps(roleService.selectRoleList(new SysRole())));
    }

    /**
     * Return departments as a flat tree-ready array for department pages.
     */
    @GetMapping("/blade-system/dept/list")
    public BladeResult<List<Map<String, Object>>> deptList() {
        return BladeResult.success(deptMaps(deptService.selectDeptList(new SysDeptView())));
    }

    /**
     * Return department options for user and department selectors.
     */
    @GetMapping("/blade-system/dept/tree")
    public BladeResult<List<Map<String, Object>>> deptTree() {
        return BladeResult.success(deptMaps(deptService.selectDeptList(new SysDeptView())));
    }

    /**
     * Return menus as a flat tree-ready array for the menu management table.
     */
    @GetMapping("/blade-system/menu/list")
    public BladeResult<List<Map<String, Object>>> menuList() {
        return BladeResult.success(menuMaps(menuService.selectMenuList(new SysMenu(), "1")));
    }

    /**
     * Return menu options for scope management dialogs.
     */
    @GetMapping("/blade-system/menu/menu-list")
    public BladeResult<List<Map<String, Object>>> menuOnlyList() {
        return BladeResult.success(menuMaps(menuService.selectMenuList(new SysMenu(), "1")));
    }

    /**
     * Return posts in selector shape for the user management post selector.
     */
    @GetMapping("/blade-system/post/select")
    public BladeResult<List<Map<String, Object>>> postSelect() {
        return BladeResult.success(postMaps(selectPosts()));
    }

    /**
     * Return posts in SpringBlade page shape for the post management table.
     */
    @GetMapping("/blade-system/post/list")
    public BladeResult<BladePage<Map<String, Object>>> postList(HttpServletRequest request) {
        List<Map<String, Object>> rows = postMaps(selectPosts());
        return BladeResult.success(toPage(rows, rows.size(), buildPageQuery(request)));
    }

    /**
     * Return the fixed tenant option used by the single-tenant deployment.
     */
    @GetMapping("/blade-system/tenant/select")
    public BladeResult<List<Map<String, Object>>> tenantSelect() {
        return BladeResult.success(tenantMaps());
    }

    /**
     * Return the fixed tenant in SpringBlade page shape for compatibility.
     */
    @GetMapping("/blade-system/tenant/list")
    public BladeResult<BladePage<Map<String, Object>>> tenantList(HttpServletRequest request) {
        List<Map<String, Object>> rows = tenantMaps();
        return BladeResult.success(toPage(rows, rows.size(), buildPageQuery(request)));
    }

    /**
     * Return an empty page until the target backend has a data-scope model.
     */
    @GetMapping("/blade-system/data-scope/list")
    public BladeResult<BladePage<Map<String, Object>>> dataScopeList(HttpServletRequest request) {
        return BladeResult.success(toPage(new ArrayList<Map<String, Object>>(), 0, buildPageQuery(request)));
    }

    /**
     * Return an empty page until the target backend has an API-scope model.
     */
    @GetMapping("/blade-system/api-scope/list")
    public BladeResult<BladePage<Map<String, Object>>> apiScopeList(HttpServletRequest request) {
        return BladeResult.success(toPage(new ArrayList<Map<String, Object>>(), 0, buildPageQuery(request)));
    }

    /**
     * Build a RuoYi user filter from legacy SpringBlade query names.
     */
    private static SysUser buildUserFilter(HttpServletRequest request) {
        SysUser user = new SysUser();
        user.setUserName(firstNonBlank(request.getParameter("account"), request.getParameter("name")));
        user.setDeptId(trimToNull(request.getParameter("deptId")));
        user.setStatus(trimToNull(request.getParameter("status")));
        return user;
    }

    /**
     * Build RuoYi PageQuery from either RuoYi or SpringBlade pagination names.
     */
    private static PageQuery buildPageQuery(HttpServletRequest request) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(parsePositiveInt(firstNonBlank(request.getParameter("pageNum"), request.getParameter("current")), 1));
        pageQuery.setPageSize(parsePositiveInt(firstNonBlank(request.getParameter("pageSize"), request.getParameter("size")), 10));
        return pageQuery;
    }

    /**
     * Convert row maps to a SpringBlade-compatible page with current page slicing for in-memory sources.
     */
    private static BladePage<Map<String, Object>> toPage(List<Map<String, Object>> rows, long total, PageQuery pageQuery) {
        List<Map<String, Object>> safeRows = rows == null ? new ArrayList<Map<String, Object>>() : rows;
        int current = pageQuery.getPageNum() == null ? 1 : pageQuery.getPageNum();
        int size = pageQuery.getPageSize() == null ? 10 : pageQuery.getPageSize();
        if (safeRows.size() == total) {
            int from = Math.max(0, Math.min(safeRows.size(), (current - 1) * size));
            int to = Math.max(from, Math.min(safeRows.size(), from + size));
            safeRows = new ArrayList<Map<String, Object>>(safeRows.subList(from, to));
        }
        return BladePage.of(safeRows, total, size, current);
    }

    /**
     * Read local RuoYi posts and keep the legacy SpringBlade response aliases.
     */
    private List<SysPost> selectPosts() {
        LambdaQueryWrapper<SysPost> query = new LambdaQueryWrapper<SysPost>()
            .orderByAsc(SysPost::getPostSort)
            .orderByAsc(SysPost::getPostName);
        return postMapper.selectList(query);
    }

    /**
     * Convert RuoYi user rows to legacy system-management aliases.
     */
    private static List<Map<String, Object>> userMaps(List<SysUser> users) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (users == null) {
            return rows;
        }
        for (SysUser user : users) {
            rows.add(userMap(user));
        }
        return rows;
    }

    /**
     * Convert a single RuoYi user to the field names expected by VLStream UI tables.
     */
    private static Map<String, Object> userMap(SysUser user) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        String account = firstNonBlank(user.getLoginId(), user.getUserName());
        String realName = firstNonBlank(user.getUserName(), user.getLoginId());
        row.put("id", user.getUserId());
        row.put("userId", user.getUserId());
        row.put("account", account);
        row.put("name", realName);
        row.put("realName", realName);
        row.put("tenantId", firstNonBlank(user.getTenantId(), SingleTenant.DEFAULT_TENANT_ID));
        row.put("deptId", user.getDeptId());
        row.put("deptName", user.getDeptName());
        row.put("postId", user.getPostId());
        row.put("postName", user.getPostId());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhonenumber());
        row.put("sex", normalizeSex(user.getSex()));
        row.put("status", normalizeStatus(user.getStatus()));
        return row;
    }

    /**
     * Convert RuoYi role rows to legacy role table and tree aliases.
     */
    private static List<Map<String, Object>> roleMaps(List<SysRole> roles) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (roles == null) {
            return rows;
        }
        for (SysRole role : roles) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", role.getRoleId());
            row.put("roleId", role.getRoleId());
            row.put("parentId", "0");
            row.put("roleName", role.getRoleName());
            row.put("roleAlias", firstNonBlank(role.getRoleKey(), role.getRoleName()));
            row.put("label", role.getRoleName());
            row.put("sort", role.getRoleSort());
            row.put("status", normalizeStatus(role.getStatus()));
            row.put("remark", role.getRemark());
            rows.add(row);
        }
        return rows;
    }

    /**
     * Convert RuoYi department rows to legacy department table and tree aliases.
     */
    private static List<Map<String, Object>> deptMaps(List<SysDeptView> depts) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (depts == null) {
            return rows;
        }
        for (SysDeptView dept : depts) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", dept.getDeptId());
            row.put("deptId", dept.getDeptId());
            row.put("parentId", firstNonBlank(dept.getParentId(), "0"));
            row.put("tenantId", dept.getTenantId());
            row.put("deptName", dept.getDeptName());
            row.put("fullName", dept.getDeptName());
            row.put("label", dept.getDeptName());
            row.put("sort", dept.getOrderNum());
            row.put("status", normalizeStatus(dept.getStatus()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Convert RuoYi menu rows to legacy menu table and selector aliases.
     */
    private static List<Map<String, Object>> menuMaps(List<SysMenu> menus) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (menus == null) {
            return rows;
        }
        for (SysMenu menu : menus) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", menu.getMenuId());
            row.put("menuId", menu.getMenuId());
            row.put("parentId", firstNonBlank(menu.getParentId(), "0"));
            row.put("name", menu.getMenuName());
            row.put("menuName", menu.getMenuName());
            row.put("label", menu.getMenuName());
            row.put("code", firstNonBlank(menu.getPerms(), menu.getPath()));
            row.put("alias", menu.getMenuName());
            row.put("path", menu.getPath());
            row.put("category", menuCategory(menu.getMenuType()));
            row.put("action", menu.getPerms());
            row.put("source", menu.getIcon());
            row.put("sort", menu.getOrderNum());
            row.put("isOpen", "0".equals(menu.getVisible()));
            row.put("status", normalizeStatus(menu.getStatus()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Convert local RuoYi post rows to legacy post table and selector aliases.
     */
    private static List<Map<String, Object>> postMaps(List<SysPost> posts) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (posts == null) {
            return rows;
        }
        for (SysPost post : posts) {
            String postName = post.getPostName();
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", post.getPostId());
            row.put("postId", post.getPostId());
            row.put("postCode", firstNonBlank(post.getPostCode(), String.valueOf(post.getPostId())));
            row.put("postName", postName);
            row.put("label", postName);
            row.put("tenantId", SingleTenant.DEFAULT_TENANT_ID);
            row.put("category", "1");
            row.put("sort", post.getPostSort());
            rows.add(row);
        }
        return rows;
    }

    /**
     * Build fixed single-tenant rows for legacy tenant selectors.
     */
    private static List<Map<String, Object>> tenantMaps() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.add(tenantMap(SingleTenant.DEFAULT_TENANT_ID));
        return rows;
    }

    /**
     * Build a single tenant row with legacy tenant management aliases.
     */
    private static Map<String, Object> tenantMap(String tenantId) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", tenantId);
        row.put("tenantId", tenantId);
        row.put("tenantName", tenantId);
        row.put("domain", "");
        row.put("linkman", "");
        row.put("contactNumber", "");
        row.put("address", "");
        row.put("status", 1);
        return row;
    }

    /**
     * Map RuoYi menu type letters to the numeric category values used by legacy forms.
     */
    private static Integer menuCategory(String menuType) {
        if ("F".equals(menuType)) {
            return 2;
        }
        if ("C".equals(menuType)) {
            return 1;
        }
        return 0;
    }

    /**
     * Normalize status into SpringBlade's enabled/disabled numeric convention.
     */
    private static Integer normalizeStatus(String status) {
        return "1".equals(status) ? 0 : 1;
    }

    /**
     * Normalize RuoYi sex values while preserving unknown legacy values.
     */
    private static String normalizeSex(String sex) {
        return trimToNull(sex) == null ? "0" : sex;
    }

    /**
     * Parse positive integer query values with a safe fallback.
     */
    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Return the first non-empty string from two candidates.
     */
    private static String firstNonBlank(String first, String second) {
        String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : trimToNull(second);
    }

    /**
     * Collapse blank strings to null for consistent alias fallback.
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
