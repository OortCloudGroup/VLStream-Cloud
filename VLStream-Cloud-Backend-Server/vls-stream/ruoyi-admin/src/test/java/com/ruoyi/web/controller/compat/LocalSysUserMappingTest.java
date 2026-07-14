/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class LocalSysUserMappingTest {

    @Test
    void sysUserUsesLocalUserTable() {
        TableName tableName = SysUser.class.getAnnotation(TableName.class);

        assertEquals("sys_user", tableName.value());
    }
}
