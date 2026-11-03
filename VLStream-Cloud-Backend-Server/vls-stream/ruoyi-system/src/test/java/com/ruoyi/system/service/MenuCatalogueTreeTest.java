package com.ruoyi.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Tag("dev")
class MenuCatalogueTreeTest {
    private final SysMenuServiceImpl service = new SysMenuServiceImpl(
        mock(SysMenuMapper.class), mock(SysRoleMapper.class), mock(SysRoleMenuMapper.class));

    @Test
    void unorderedRootsMissingParentsAndCyclesNeverHideGrantedActions() {
        List<Tree<String>> tree = service.buildMenuTreeSelect(Arrays.asList(
            menu(2, "1"), menu(1, "0"), menu(3, "99"), menu(4, "5"), menu(5, "4")));
        Set<String> ids = new HashSet<>();
        collect(tree, ids);
        assertEquals(new HashSet<>(Arrays.asList("1", "2", "3", "4", "5")), ids);
    }

    private void collect(List<Tree<String>> nodes, Set<String> ids) {
        for (Tree<String> node : nodes) {
            assertTrue(ids.add(node.getId()));
            if (node.getChildren() != null) collect(node.getChildren(), ids);
        }
    }

    private SysMenu menu(long id, String parent) {
        SysMenu m = new SysMenu();
        m.setMenuId(id);
        m.setParentId(parent);
        m.setMenuName("menu" + id);
        m.setOrderNum(1);
        return m;
    }
}
