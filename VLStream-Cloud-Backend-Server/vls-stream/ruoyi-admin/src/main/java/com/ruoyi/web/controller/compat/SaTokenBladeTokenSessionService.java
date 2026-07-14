/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
