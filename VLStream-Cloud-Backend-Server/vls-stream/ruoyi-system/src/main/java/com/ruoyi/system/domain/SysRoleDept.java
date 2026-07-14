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
 * 角色和部门关联 sys_role_dept
 *
 * @author Lion Li
 */

@Data
@TableName("sys_role_dept")
public class SysRoleDept {

    /**
     * 角色ID
     */
    @TableId(type = IdType.INPUT)
    private String roleId;

    /**
     * 部门ID
     */
    private String deptId;

}
