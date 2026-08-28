/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TreeBuildUtils;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.domain.vo.MetaVo;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * menu layer Process
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper baseMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * userQuery menu list
     *
     * @param userId user ID
     * @return menu
     */
    @Override
    public List<SysMenu> selectMenuList(String userId) {
        return selectMenuList(new SysMenu(), userId);
    }

    /**
     * Query menu list
     *
     * @param menu menuinfo
     * @return menu
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, String userId) {
        List<SysMenu> menuList = null;
        // administrator all menuinfo
        if (LoginHelper.isAdmin(userId)) {
            menuList = baseMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .like(StringUtils.isNotBlank(menu.getMenuName()), SysMenu::getMenuName, menu.getMenuName())
                .eq(StringUtils.isNotBlank(menu.getVisible()), SysMenu::getVisible, menu.getVisible())
                .eq(StringUtils.isNotBlank(menu.getStatus()), SysMenu::getStatus, menu.getStatus())
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        } else {
            QueryWrapper<SysMenu> wrapper = Wrappers.query();
            wrapper.eq("sur.user_id", userId)
                .like(StringUtils.isNotBlank(menu.getMenuName()), "m.menu_name", menu.getMenuName())
                .eq(StringUtils.isNotBlank(menu.getVisible()), "m.visible", menu.getVisible())
                .eq(StringUtils.isNotBlank(menu.getStatus()), "m.status", menu.getStatus())
                .orderByAsc("m.parent_id")
                .orderByAsc("m.order_num");
            menuList = baseMapper.selectMenuListByUserId(wrapper);
        }
        return menuList;
    }

    /**
     * user IDQuery
     *
     * @param userId user ID
     * @return
     */
    @Override
    public Set<String> selectMenuPermsByUserId(String userId) {
        List<String> perms = baseMapper.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.trim()));
            }
        }
        return permsSet;
    }

    /**
     * role IDQuery
     *
     * @param roleId role ID
     * @return
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId) {
        List<String> perms = baseMapper.selectMenuPermsByRoleId(roleId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.trim()));
            }
        }
        return permsSet;
    }

    /**
     * user IDQuery menu
     *
     * @param userId username
     * @return menu
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(String userId) {
        List<SysMenu> menus = null;
        if (LoginHelper.isAdmin(userId)) {
            menus = baseMapper.selectMenuTreeAll();
        } else {
            menus = baseMapper.selectMenuTreeByUserId(userId);
        }
//        List<SysMenu> menus = baseMapper.selectMenuTreeAll();
        return getChildPerms(menus, "0");
    }

    /**
     * role IDQuery menu info
     *
     * @param roleId role ID
     * @return in menu
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        return baseMapper.selectMenuListByRoleId(roleId, role.getMenuCheckStrictly());
    }

    /**
     * Build before need to menu
     *
     * @param menus menu
     * @return
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setQuery(menu.getQueryParam());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            List<SysMenu> cMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StringUtils.capitalize(menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQueryParam());
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().equals("0")  && isInnerLink(menu)) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(StringUtils.capitalize(routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * Build before need to
     *
     * @param menus menu
     * @return
     */
    @Override
    public List<Tree<String>> buildMenuTreeSelect(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.build(menus, (menu, tree) ->
            tree.setId(String.valueOf(menu.getMenuId()))
                .setParentId(menu.getParentId())
                .setName(menu.getMenuName())
                .setWeight(menu.getOrderNum()));
    }

    /**
     * menu IDQuery info
     *
     * @param menuId menu ID
     * @return menuinfo
     */
    @Override
    public SysMenu selectMenuById(Long menuId) {
        return baseMapper.selectById(menuId);
    }

    /**
     * whether in menu sub node
     *
     * @param menuId menu ID
     * @return
     */
    @Override
    public boolean hasChildByMenuId(Long menuId) {
        return baseMapper.exists(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
    }

    /**
     * Query menu
     *
     * @param menuId menu ID
     * @return
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        return roleMenuMapper.exists(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, menuId));
    }

    /**
     * Add menuinfo
     *
     * @param menu menuinfo
     * @return
     */
    @Override
    public int insertMenu(SysMenu menu) {
        return baseMapper.insert(menu);
    }

    /**
     * Update menuinfo
     *
     * @param menu menuinfo
     * @return
     */
    @Override
    public int updateMenu(SysMenu menu) {
        return baseMapper.updateById(menu);
    }

    /**
     * Delete menu info
     *
     * @param menuId menu ID
     * @return
     */
    @Override
    public int deleteMenuById(Long menuId) {
        return baseMapper.deleteById(menuId);
    }

    /**
     * Validate menu namewhether
     *
     * @param menu menuinfo
     * @return
     */
    @Override
    public boolean checkMenuNameUnique(SysMenu menu) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getMenuName, menu.getMenuName())
            .eq(SysMenu::getParentId, menu.getParentId())
            .ne(ObjectUtil.isNotNull(menu.getMenuId()), SysMenu::getMenuId, menu.getMenuId()));
        return !exist;
    }

    /**
     * Get
     *
     * @param menu menuinfo
     * @return
     */
    public String getRouteName(SysMenu menu) {
        String routerName = StringUtils.capitalize(menu.getPath());
        // non- and is ( to )
        if (isMenuFrame(menu)) {
            routerName = StringUtils.EMPTY;
        }
        return routerName;
    }

    /**
     * Get
     *
     * @param menu menuinfo
     * @return
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = menu.getPath();
        //
        if (!menu.getParentId().equals("0")  && isInnerLink(menu)) {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // non- and is ( to )
        if (menu.getParentId().equals("0") && UserConstants.TYPE_DIR.equals(menu.getMenuType())
            && UserConstants.NO_FRAME.equals(menu.getIsFrame())) {
            routerPath = "/" + menu.getPath();
        }
        // non- and is ( to menu)
        else if (isMenuFrame(menu)) {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * Get componentinfo
     *
     * @param menu menuinfo
     * @return componentinfo
     */
    public String getComponent(SysMenu menu) {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu)) {
            component = menu.getComponent();
        } else if (StringUtils.isEmpty(menu.getComponent()) && !menu.getParentId().equals("0") && isInnerLink(menu)) {
            component = UserConstants.INNER_LINK;
        } else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu)) {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * whether to menu
     *
     * @param menu menuinfo
     * @return
     */
    public boolean isMenuFrame(SysMenu menu) {
        return menu.getParentId().equals("0") && UserConstants.TYPE_MENU.equals(menu.getMenuType())
            && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * whether to component
     *
     * @param menu menuinfo
     * @return
     */
    public boolean isInnerLink(SysMenu menu) {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StringUtils.ishttp(menu.getPath());
    }

    /**
     * whether to parent_viewcomponent
     *
     * @param menu menuinfo
     * @return
     */
    public boolean isParentView(SysMenu menu) {
        return !menu.getParentId().equals("0")  && UserConstants.TYPE_DIR.equals(menu.getMenuType());


    }

    /**
     * node IDGet all sub node
     *
     * @param list
     * @param parentId nodeID
     * @return String
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, String  parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (SysMenu t : list) {
            // 、 nodeID, node all sub node
            if (t.getParentId().equals(parentId)) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     *
     *
     * @param list
     * @param t
     */
    private void recursionFn(List<SysMenu> list, SysMenu t) {
        // sub node
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * sub node
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
        return StreamUtils.filter(list, n -> n.getParentId().equals(String.valueOf(t.getMenuId())));
    }

    /**
     * Check whether sub node
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        return CollUtil.isNotEmpty(getChildList(list, t));
    }

    /**
     * Replace
     */
    public String innerLinkReplaceEach(String path) {
        return StringUtils.replaceEach(path, new String[]{Constants.HTTP, Constants.HTTPS, Constants.WWW, "."},
            new String[]{"", "", "", "/"});
    }
}
