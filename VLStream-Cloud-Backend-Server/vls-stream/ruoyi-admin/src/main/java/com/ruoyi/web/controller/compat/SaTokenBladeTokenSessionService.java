/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class SaTokenBladeTokenSessionService implements BladeTokenSessionService {

    @Override
    public void logoutByToken(String token) {
        StpUtil.logoutByTokenValue(token);
    }
}
