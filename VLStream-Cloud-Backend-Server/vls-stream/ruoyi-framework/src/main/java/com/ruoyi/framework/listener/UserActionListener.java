/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.listener;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.dto.UserOnlineDTO;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * user to
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserActionListener implements SaTokenListener {

    private final SaTokenConfig tokenConfig;

    /**
     *
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        UserType userType = UserType.getUserType(loginId.toString());
        if (userType == UserType.SYS_USER) {
            UserAgent userAgent = UserAgentUtil.parse(ServletUtils.getRequest().getHeader("User-Agent"));
            String ip = ServletUtils.getClientIP();
            LoginUser user = LoginHelper.getLoginUser();
            UserOnlineDTO dto = new UserOnlineDTO();
            dto.setIpaddr(ip);
            dto.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
            dto.setBrowser(userAgent.getBrowser().getName());
            dto.setOs(userAgent.getOs().getName());
            dto.setLoginTime(System.currentTimeMillis());
            dto.setTokenId(tokenValue);
            dto.setUserName(user.getUsername());
            dto.setDeptName(user.getDeptName());
            if(tokenConfig.getTimeout() == -1) {
                RedisUtils.setCacheObject(CacheConstants.ONLINE_TOKEN_KEY + tokenValue, dto);
            } else {
                RedisUtils.setCacheObject(CacheConstants.ONLINE_TOKEN_KEY + tokenValue, dto, Duration.ofSeconds(tokenConfig.getTimeout()));
            }
            log.info("user doLogin, userId:{}, token:{}", loginId, tokenValue);
        } else if (userType == UserType.APP_USER) {
            // app
        }
    }

    /**
     *
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheConstants.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogout, userId:{}, token:{}", loginId, tokenValue);
    }

    /**
     *
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheConstants.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogoutByLoginId, userId:{}, token:{}", loginId, tokenValue);
    }

    /**
     *
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheConstants.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doReplaced, userId:{}, token:{}", loginId, tokenValue);
    }

    /**
     *
     */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
    }

    /**
     *
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
    }

    /**
     *
     */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
    }

    /**
     * Session
     */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
    }

    /**
     * Session
     */
    @Override
    public void doCreateSession(String id) {
    }

    /**
     * Session
     */
    @Override
    public void doLogoutSession(String id) {
    }

    /**
     * Token
     */
    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
    }
}
