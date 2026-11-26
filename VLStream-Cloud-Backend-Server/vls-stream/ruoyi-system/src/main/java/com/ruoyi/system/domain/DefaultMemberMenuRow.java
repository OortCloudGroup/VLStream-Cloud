package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.entity.SysMenu;
import lombok.Data;

/** Catalogue row read for default member menu synchronization (sys_menu has no tenant field on the entity). */
@Data
public class DefaultMemberMenuRow {
    private Long menuId;
    private String parentId;
    private String path;
    private String perms;
    private String status;
    private String tenantId;

    public SysMenu toMenu() {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setPath(path);
        menu.setPerms(perms);
        menu.setStatus(status);
        return menu;
    }
}
