/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class BladeSystemDirectoryCompatControllerTest {

    @Test
    void userListReturnsBladePageWithFrontendAliases() {
        BladeSystemDirectoryCompatController controller = newController();
        SysUser user = new SysUser();
        user.setUserId("user-1");
        user.setUserName("workflow-admin");
        user.setLoginId("admin");
        user.setTenantId("tenant-a");
        user.setDeptName("Operations");
        user.setEmail("admin@example.com");
        user.setPhonenumber("13800000000");
        when(userService.selectPageUserList(any(SysUser.class), any())).thenReturn(new TableDataInfo<SysUser>(Arrays.asList(user), 1));

        BladeResult<BladePage<Map<String, Object>>> result = controller.userList(new MockHttpServletRequest());
        Map<String, Object> row = result.getData().getRecords().get(0);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        assertEquals("user-1", row.get("id"));
        assertEquals("admin", row.get("account"));
        assertEquals("workflow-admin", row.get("realName"));
        assertEquals("Operations", row.get("deptName"));
    }

    @Test
    void directoryEndpointsReturnArraysOrPagesForSystemViews() {
        BladeSystemDirectoryCompatController controller = newController();
        SysRole role = new SysRole();
        role.setRoleId("role-1");
        role.setRoleName("Administrator");
        role.setRoleKey("admin");
        role.setRoleSort(1);
        SysDeptView dept = new SysDeptView();
        dept.setDeptId("dept-1");
        dept.setDeptName("Operations");
        dept.setParentId("0");
        SysMenu menu = new SysMenu();
        menu.setMenuId(100L);
        menu.setMenuName("System");
        menu.setMenuType("M");
        menu.setParentId("0");
        SysPost post = new SysPost();
        post.setPostId(1L);
        post.setPostName("Director");
        post.setPostCode("director");
        SysUser tenantUser = new SysUser();
        tenantUser.setTenantId("tenant-a");

        when(roleService.selectRoleList(any(SysRole.class))).thenReturn(Arrays.asList(role));
        when(deptService.selectDeptList(any(SysDeptView.class))).thenReturn(Arrays.asList(dept));
        when(menuService.selectMenuList(any(SysMenu.class), any())).thenReturn(Arrays.asList(menu));
        when(postMapper.selectList(any())).thenReturn(Arrays.asList(post));
        when(userService.selectUserList(any(SysUser.class))).thenReturn(Arrays.asList(tenantUser));

        assertEquals("Administrator", controller.roleList().getData().get(0).get("roleName"));
        assertEquals("Operations", controller.deptTree().getData().get(0).get("deptName"));
        assertEquals("System", controller.menuList().getData().get(0).get("name"));
        assertEquals("Director", controller.postSelect().getData().get(0).get("postName"));
        assertEquals("000000", controller.tenantSelect().getData().get(0).get("tenantId"));
        assertEquals(0, controller.dataScopeList(new MockHttpServletRequest()).getData().getTotal());
        assertEquals(0, controller.apiScopeList(new MockHttpServletRequest()).getData().getTotal());
    }

    @Test
    void exposesRoutesUsedBySystemManagementPages() throws Exception {
        assertGetRoute("userList", "/blade-system/user/list", javax.servlet.http.HttpServletRequest.class);
        assertGetRoute("postSelect", "/blade-system/post/select");
        assertGetRoute("postList", "/blade-system/post/list", javax.servlet.http.HttpServletRequest.class);
        assertGetRoute("tenantSelect", "/blade-system/tenant/select");
        assertGetRoute("tenantList", "/blade-system/tenant/list", javax.servlet.http.HttpServletRequest.class);
        assertGetRoute("roleTree", "/blade-system/role/tree");
        assertGetRoute("roleList", "/blade-system/role/list");
        assertGetRoute("deptTree", "/blade-system/dept/tree");
        assertGetRoute("deptList", "/blade-system/dept/list");
        assertGetRoute("menuList", "/blade-system/menu/list");
        assertGetRoute("menuOnlyList", "/blade-system/menu/menu-list");
        assertGetRoute("dataScopeList", "/blade-system/data-scope/list", javax.servlet.http.HttpServletRequest.class);
        assertGetRoute("apiScopeList", "/blade-system/api-scope/list", javax.servlet.http.HttpServletRequest.class);
    }

    private final ISysUserService userService = mock(ISysUserService.class);
    private final ISysRoleService roleService = mock(ISysRoleService.class);
    private final ISysDeptService deptService = mock(ISysDeptService.class);
    private final ISysMenuService menuService = mock(ISysMenuService.class);
    private final SysPostMapper postMapper = mock(SysPostMapper.class);

    private BladeSystemDirectoryCompatController newController() {
        return new BladeSystemDirectoryCompatController(userService, roleService, deptService, menuService, postMapper);
    }

    private static void assertGetRoute(String methodName, String path, Class<?>... parameterTypes) throws Exception {
        Method method = BladeSystemDirectoryCompatController.class.getDeclaredMethod(methodName, parameterTypes);

        assertArrayEquals(new String[] {path}, method.getAnnotation(GetMapping.class).value());
    }
}
