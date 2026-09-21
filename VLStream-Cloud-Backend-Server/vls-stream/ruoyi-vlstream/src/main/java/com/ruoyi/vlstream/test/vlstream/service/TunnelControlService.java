/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class TunnelControlService {
    private final TunnelEndpointMapper endpointMapper;
    private final TunnelTokenService tokenService;
    private final VlsTunnelProperties properties;

    public TunnelControlService(TunnelEndpointMapper endpointMapper,
                                TunnelTokenService tokenService,
                                VlsTunnelProperties properties) {
        this.endpointMapper = endpointMapper;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    public List<TunnelDtos.RouteView> desiredRoutes() {
        requireControlConfiguration();
        List<TunnelDtos.RouteView> routes = new ArrayList<TunnelDtos.RouteView>();
        for (TunnelEndpoint endpoint : endpointMapper.selectDesiredRoutes()) {
            TunnelDtos.RouteView route = new TunnelDtos.RouteView();
            route.setEndpointId(String.valueOf(endpoint.getId()));
            route.setServiceName(endpoint.getServiceName());
            route.setServiceToken(tokenService.deriveServiceToken(endpoint.getId(), endpoint.getConfigGeneration()));
            route.setBindAddr(properties.getServerBindHost() + ":" + endpoint.getServerBindPort());
            route.setConfigGeneration(endpoint.getConfigGeneration());
            route.setDesiredState(endpoint.getDesiredState());
            routes.add(route);
        }
        return routes;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reportRouteStatus(Long endpointId, TunnelDtos.RouteStatusRequest request) {
        TunnelEndpoint endpoint = endpointMapper.selectByIdGlobal(endpointId);
        if (endpoint == null) {
            throw new TunnelApiException(404, "隧道路由不存在");
        }
        if (!endpoint.getConfigGeneration().equals(request.getConfigGeneration())) {
            throw new TunnelApiException(409, "路由状态对应的配置版本已过期");
        }
        String routeStatus = StringUtils.upperCase(StringUtils.trimToEmpty(request.getRouteStatus()), Locale.ROOT);
        if (!"APPLIED".equals(routeStatus) && !"FAILED".equals(routeStatus)
            && !"PENDING".equals(routeStatus)) {
            throw new TunnelApiException(400, "路由状态不合法");
        }
        endpoint.setRouteStatus(routeStatus);
        endpoint.setRouteAppliedGeneration("APPLIED".equals(routeStatus)
            ? request.getConfigGeneration() : null);
        endpoint.setLastErrorCode("FAILED".equals(routeStatus) ? "ROUTE_APPLY_FAILED" : null);
        endpoint.setLastErrorMessage("FAILED".equals(routeStatus)
            ? StringUtils.abbreviate(StringUtils.trimToNull(request.getErrorMessage()), 255) : null);
        endpoint.setUpdateTime(new Date());
        TunnelTenantScope.run(endpoint.getTenantId(), new Runnable() {
            @Override
            public void run() {
                endpointMapper.updateById(endpoint);
            }
        });
    }

    public void verifyControlToken(String token) {
        if (StringUtils.length(properties.getControlApiToken()) < 32
            || !tokenService.secureEquals(properties.getControlApiToken(), token)) {
            throw new TunnelApiException(401, "隧道控制服务认证失败");
        }
    }

    private void requireControlConfiguration() {
        if (!properties.isEnabled()) {
            throw new TunnelApiException(503, "IPC远程管理功能未启用");
        }
        if (!"127.0.0.1".equals(properties.getServerBindHost())
            && !"localhost".equalsIgnoreCase(properties.getServerBindHost())) {
            throw new TunnelApiException(503, "rathole服务映射必须绑定回环地址");
        }
        tokenService.deriveServiceToken(0L, 0L);
    }
}
