package com.ruoyi.web.controller.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.framework.config.properties.TokenProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/** Bounds local multi-tenant sessions without consulting the platform for local resources. */
@Component
public class LocalSessionLifetimeInterceptor implements HandlerInterceptor {
    private static final long DEFAULT_MAX_AGE_SECONDS = 86400L;
    private final TokenProperties tokenProperties;
    private final long maxAgeSeconds;

    public LocalSessionLifetimeInterceptor(TokenProperties tokenProperties,
            @Value("${token.local-session-max-age-seconds:86400}") long configuredMaxAgeSeconds) {
        this.tokenProperties = tokenProperties;
        this.maxAgeSeconds = configuredMaxAgeSeconds > 0 ? configuredMaxAgeSeconds : DEFAULT_MAX_AGE_SECONDS;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }
        if (!TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType())
                || WebMvcConfigToken.isPublicMarketRead(request.getMethod(), path)) {
            return true;
        }
        StpUtil.checkLogin();
        // Do not create a replacement session when the trusted server-side session has disappeared.
        SaSession session = StpUtil.getStpLogic().getTokenSessionByToken(StpUtil.getTokenValue(), false);
        long now = System.currentTimeMillis();
        if (session == null || session.getCreateTime() <= 0 || session.getCreateTime() > now
                || TimeUnit.MILLISECONDS.toSeconds(now - session.getCreateTime()) >= maxAgeSeconds) {
            throw new NotLoginException("本地会话已过期，请重新登录", StpUtil.getLoginType(), NotLoginException.TOKEN_TIMEOUT);
        }
        return true;
    }
}
