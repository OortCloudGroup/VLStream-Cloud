/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * role and menu sys_role_menu
 *
 * @author Lion Li
 */

@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /**
     * tenant ID.
     */
    private String tenantId;

    /**
     * role ID
     */
    @TableId(type = IdType.INPUT)
    private String  roleId;

    /**
     * menu ID
     */
    private Long menuId;

}
