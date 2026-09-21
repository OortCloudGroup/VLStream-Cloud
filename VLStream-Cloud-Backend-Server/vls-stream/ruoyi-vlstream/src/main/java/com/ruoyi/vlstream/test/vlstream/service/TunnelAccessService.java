/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelAccessSessionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelAccessSession;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.UUID;

@Service
public class TunnelAccessService {
    private final TunnelEndpointMapper endpointMapper;
    private final TunnelAccessSessionMapper sessionMapper;
    private final TunnelTokenService tokenService;
    private final VlsTunnelProperties properties;

    public TunnelAccessService(TunnelEndpointMapper endpointMapper,
                               TunnelAccessSessionMapper sessionMapper,
                               TunnelTokenService tokenService,
                               VlsTunnelProperties properties) {
        this.endpointMapper = endpointMapper;
        this.sessionMapper = sessionMapper;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Transactional(rollbackFor = Exception.class)
    public TunnelDtos.AccessSessionView createSession(String rawDeviceId) {
        if (!properties.isEnabled()) {
            throw new ServiceException("IPC远程管理功能未启用");
        }
        requireGatewayBaseUrl();
        String deviceId = StringUtils.trimToEmpty(rawDeviceId);
        TunnelEndpoint endpoint = endpointMapper.selectOne(new LambdaQueryWrapper<TunnelEndpoint>()
            .eq(TunnelEndpoint::getDeviceId, deviceId)
            .eq(TunnelEndpoint::getIsDeleted, 0)
            .last("limit 1"));
        if (endpoint == null) {
            throw new ServiceException("该设备尚未配置远程管理");
        }
        requireAvailable(endpoint);

        Date now = new Date();
        String rawToken = tokenService.randomToken(32);
        TunnelAccessSession session = new TunnelAccessSession();
        session.setId(IdWorker.getId());
        session.setTenantId(endpoint.getTenantId());
        session.setSessionId(UUID.randomUUID().toString());
        session.setEndpointId(endpoint.getId());
        session.setDeviceId(endpoint.getDeviceId());
        session.setUserId(StringUtils.defaultIfBlank(LoginHelper.getUserId(), LoginHelper.getUsername()));
        session.setAccessTokenHash(tokenService.hash(rawToken));
        session.setExpiresAt(new Date(now.getTime() + normalizedSessionTtl() * 1000L));
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);

        TunnelDtos.AccessSessionView view = new TunnelDtos.AccessSessionView();
        view.setSessionId(session.getSessionId());
        view.setExpiresAt(session.getExpiresAt());
        view.setAccessUrl(buildAccessUrl(session.getSessionId(), rawToken));
        return view;
    }

    @Transactional(rollbackFor = Exception.class)
    public TunnelDtos.SessionRouteView resolveSession(String rawAccessToken) {
        if (StringUtils.isBlank(rawAccessToken)) {
            throw new TunnelApiException(401, "缺少远程访问令牌");
        }
        TunnelAccessSession session = sessionMapper.selectByTokenHash(tokenService.hash(rawAccessToken));
        Date now = new Date();
        if (session == null || session.getRevokedAt() != null || session.getExpiresAt() == null
            || !session.getExpiresAt().after(now)) {
            throw new TunnelApiException(401, "远程访问会话无效或已过期");
        }
        if (session.getOpenedAt() != null) {
            throw new TunnelApiException(401, "远程访问启动令牌已使用");
        }
        TunnelEndpoint endpoint = endpointMapper.selectByIdGlobal(session.getEndpointId());
        if (endpoint == null) {
            throw new TunnelApiException(404, "远程管理端点不存在");
        }
        try {
            requireAvailable(endpoint);
        } catch (ServiceException exception) {
            throw new TunnelApiException(503, exception.getMessage());
        }
        session.setOpenedAt(now);
        session.setUpdateTime(now);
        TunnelTenantScope.run(session.getTenantId(), new Runnable() {
            @Override
            public void run() {
                sessionMapper.updateById(session);
            }
        });

        TunnelDtos.SessionRouteView view = new TunnelDtos.SessionRouteView();
        view.setSessionId(session.getSessionId());
        view.setEndpointId(String.valueOf(endpoint.getId()));
        view.setUpstreamHost(properties.getServerBindHost());
        view.setUpstreamPort(endpoint.getServerBindPort());
        view.setExpiresAt(session.getExpiresAt());
        return view;
    }

    public void verifyGatewayToken(String token) {
        if (StringUtils.length(properties.getGatewayApiToken()) < 32
            || !tokenService.secureEquals(properties.getGatewayApiToken(), token)) {
            throw new TunnelApiException(401, "远程管理网关认证失败");
        }
    }

    String buildAccessUrl(String sessionId, String rawToken) {
        requireGatewayBaseUrl();
        String sessionBaseUrl = StringUtils.replace(
            StringUtils.removeEnd(properties.getGatewayBaseUrl().trim(), "/"),
            "{sessionId}", sessionId);
        return UriComponentsBuilder.fromHttpUrl(sessionBaseUrl)
            .pathSegment("s", rawToken)
            .build().encode().toUriString();
    }

    private void requireAvailable(TunnelEndpoint endpoint) {
        if (!"ENABLED".equals(endpoint.getDesiredState())) {
            throw new ServiceException("设备远程管理当前未启用");
        }
        if (!"APPLIED".equals(endpoint.getRouteStatus())
            || !endpoint.getConfigGeneration().equals(endpoint.getRouteAppliedGeneration())) {
            throw new ServiceException("设备隧道路由尚未生效");
        }
        long heartbeatCutoff = System.currentTimeMillis()
            - Math.max(30, properties.getHeartbeatTimeoutSeconds()) * 1000L;
        if (endpoint.getLastHeartbeatAt() == null
            || endpoint.getLastHeartbeatAt().getTime() < heartbeatCutoff) {
            throw new ServiceException("设备远程管理Agent离线");
        }
        if (!"ONLINE".equals(endpoint.getTunnelStatus())) {
            throw new ServiceException("设备隧道尚未连接");
        }
        if (!"AVAILABLE".equals(endpoint.getLocalWebStatus())) {
            throw new ServiceException("IPC本地管理后台不可用");
        }
    }

    private int normalizedSessionTtl() {
        return Math.max(60, Math.min(900, properties.getAccessSessionTtlSeconds()));
    }

    private void requireGatewayBaseUrl() {
        if (StringUtils.isBlank(properties.getGatewayBaseUrl())
            || !properties.getGatewayBaseUrl().contains("{sessionId}")) {
            throw new ServiceException("远程管理HTTPS网关地址必须包含{sessionId}通配子域占位符");
        }
        if (!StringUtils.startsWithIgnoreCase(properties.getGatewayBaseUrl(), "https://")) {
            throw new ServiceException("远程管理网关必须使用HTTPS");
        }
    }
}
