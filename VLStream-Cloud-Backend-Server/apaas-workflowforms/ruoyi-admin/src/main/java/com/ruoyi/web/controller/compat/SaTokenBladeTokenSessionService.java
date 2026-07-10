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
