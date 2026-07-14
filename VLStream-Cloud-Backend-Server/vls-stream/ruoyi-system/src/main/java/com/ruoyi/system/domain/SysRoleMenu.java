/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * @author Lion Li
 */

@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /**
     * 角色ID
     */
    @TableId(type = IdType.INPUT)
    private String  roleId;

    /**
     * 菜单ID
     */
    private Long menuId;

}
