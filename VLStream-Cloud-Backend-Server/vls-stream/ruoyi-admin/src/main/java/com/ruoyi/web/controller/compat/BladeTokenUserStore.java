/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;

public interface BladeTokenUserStore {

    SysUser get(String token);

    void put(String token, SysUser user, long timeoutSeconds);

    void remove(String token);
}
