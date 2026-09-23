/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelAccessSessionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEnrollmentMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelAccessSession;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEnrollment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;

@Service
public class TunnelManagementService {
    private final TunnelEndpointMapper endpointMapper;
    private final TunnelEnrollmentMapper enrollmentMapper;
    private final TunnelAccessSessionMapper accessSessionMapper;
    private final WvpVlStreamDeviceResolver deviceResolver;
    private final TunnelTokenService tokenService;
    private final VlsTunnelProperties properties;

    public TunnelManagementService(TunnelEndpointMapper endpointMapper,
                                   TunnelEnrollmentMapper enrollmentMapper,
                                   TunnelAccessSessionMapper accessSessionMapper,
                                   WvpVlStreamDeviceResolver deviceResolver,
                                   TunnelTokenService tokenService,
                                   VlsTunnelProperties properties) {
        this.endpointMapper = endpointMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.accessSessionMapper = accessSessionMapper;
        this.deviceResolver = deviceResolver;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized TunnelDtos.EnrollmentView issueEnrollment(String rawDeviceId) {
        requireEnabled();
        String deviceId = StringUtils.trimToEmpty(rawDeviceId);
        if (deviceId.isEmpty() || deviceId.length() > 100) {
            throw new ServiceException("设备业务ID不能为空且长度不能超过100");
        }
        String tenantId = currentTenantId();
        DeviceInfo device = deviceResolver.resolve(deviceId);
        if (!StringUtils.equals(tenantId, device.getTenantId())) {
            throw new ServiceException("设备不属于当前租户，禁止签发远程管理激活码");
        }
        Date now = new Date();
        TunnelEndpoint endpoint = endpointMapper.selectOne(new LambdaQueryWrapper<TunnelEndpoint>()
            .eq(TunnelEndpoint::getDeviceId, deviceId)
            .eq(TunnelEndpoint::getIsDeleted, 0)
            .last("limit 1"));
        if (endpoint == null) {
            endpoint = new TunnelEndpoint();
            endpoint.setId(IdWorker.getId());
            endpoint.setTenantId(tenantId);
            endpoint.setWvpDeviceRowId(device.getId());
            endpoint.setDeviceId(deviceId);
            endpoint.setDeviceName(device.getDeviceName());
            endpoint.setLocalWebScheme("http");
            endpoint.setLocalWebPort(80);
            endpoint.setServerBindPort(allocatePort());
            endpoint.setServiceName("ipc_" + endpoint.getId());
            endpoint.setConfigGeneration(1L);
            endpoint.setDesiredState("ENABLED");
            endpoint.setAgentStatus("WAITING_FOR_AGENT");
            endpoint.setTunnelStatus("OFFLINE");
            endpoint.setLocalWebStatus("UNKNOWN");
            endpoint.setRouteStatus("PENDING");
            endpoint.setCreateUser(LoginHelper.getUserId());
            endpoint.setCreateDept(LoginHelper.getDeptId());
            endpoint.setCreateTime(now);
            endpoint.setUpdateTime(now);
            endpoint.setStatus(1);
            endpoint.setIsDeleted(0);
            endpointMapper.insert(endpoint);
        } else {
            endpoint.setWvpDeviceRowId(device.getId());
            endpoint.setDeviceName(device.getDeviceName());
            endpoint.setAgentTokenHash(null);
            endpoint.setAgentInstanceId(null);
            endpoint.setDesiredState("ENABLED");
            endpoint.setAgentStatus("WAITING_FOR_AGENT");
            endpoint.setTunnelStatus("OFFLINE");
            endpoint.setLocalWebStatus("UNKNOWN");
            endpoint.setRouteStatus("PENDING");
            endpoint.setRouteAppliedGeneration(null);
            endpoint.setRevokedAt(null);
            endpoint.setConfigGeneration(nextGeneration(endpoint.getConfigGeneration()));
            endpoint.setUpdateUser(LoginHelper.getUserId());
            endpoint.setUpdateTime(now);
            endpointMapper.updateById(endpoint);
            accessSessionMapper.update(null, new LambdaUpdateWrapper<TunnelAccessSession>()
                .eq(TunnelAccessSession::getEndpointId, endpoint.getId())
                .isNull(TunnelAccessSession::getRevokedAt)
                .set(TunnelAccessSession::getRevokedAt, now)
                .set(TunnelAccessSession::getUpdateTime, now));
        }

        enrollmentMapper.update(null, new LambdaUpdateWrapper<TunnelEnrollment>()
            .eq(TunnelEnrollment::getEndpointId, endpoint.getId())
            .isNull(TunnelEnrollment::getConsumedAt)
            .eq(TunnelEnrollment::getIsDeleted, 0)
            .set(TunnelEnrollment::getIsDeleted, 1)
            .set(TunnelEnrollment::getUpdateTime, now));

        String code = tokenService.randomToken(24);
        TunnelEnrollment enrollment = new TunnelEnrollment();
        enrollment.setId(IdWorker.getId());
        enrollment.setTenantId(tenantId);
        enrollment.setEndpointId(endpoint.getId());
        enrollment.setDeviceId(deviceId);
        enrollment.setCodeHash(tokenService.hash(code));
        enrollment.setExpiresAt(new Date(now.getTime() + normalizedEnrollmentTtl() * 1000L));
        enrollment.setCreateUser(LoginHelper.getUserId());
        enrollment.setCreateTime(now);
        enrollment.setUpdateTime(now);
        enrollment.setIsDeleted(0);
        enrollmentMapper.insert(enrollment);

        TunnelDtos.EnrollmentView view = new TunnelDtos.EnrollmentView();
        view.setDeviceId(deviceId);
        view.setEnrollmentCode(code);
        view.setExpiresAt(enrollment.getExpiresAt());
        return view;
    }

    public TunnelDtos.EndpointView getEndpoint(String rawDeviceId) {
        String deviceId = StringUtils.trimToEmpty(rawDeviceId);
        if (StringUtils.isBlank(deviceId)) {
            throw new ServiceException("设备业务ID不能为空");
        }
        TunnelEndpoint endpoint = endpointMapper.selectOne(new LambdaQueryWrapper<TunnelEndpoint>()
            .eq(TunnelEndpoint::getDeviceId, deviceId)
            .eq(TunnelEndpoint::getIsDeleted, 0)
            .last("limit 1"));
        if (endpoint == null) {
            TunnelDtos.EndpointView view = new TunnelDtos.EndpointView();
            view.setFeatureEnabled(properties.isEnabled());
            view.setConfigured(false);
            view.setDeviceId(deviceId);
            return view;
        }
        return toView(endpoint);
    }

    @Transactional(rollbackFor = Exception.class)
    public TunnelDtos.EndpointView changeDesiredState(String rawDeviceId, String rawState) {
        TunnelEndpoint endpoint = requireEndpoint(StringUtils.trimToEmpty(rawDeviceId));
        String state = StringUtils.upperCase(StringUtils.trimToEmpty(rawState), Locale.ROOT);
        if (!"ENABLED".equals(state) && !"DISABLED".equals(state) && !"REVOKED".equals(state)) {
            throw new ServiceException("期望状态只能是 ENABLED、DISABLED 或 REVOKED");
        }
        Date now = new Date();
        endpoint.setDesiredState(state);
        endpoint.setConfigGeneration(nextGeneration(endpoint.getConfigGeneration()));
        endpoint.setRouteStatus("PENDING");
        endpoint.setRouteAppliedGeneration(null);
        endpoint.setUpdateUser(LoginHelper.getUserId());
        endpoint.setUpdateTime(now);
        if ("REVOKED".equals(state)) {
            endpoint.setRevokedAt(now);
            endpoint.setAgentStatus("REVOKED");
            endpoint.setTunnelStatus("OFFLINE");
        } else if ("DISABLED".equals(state)) {
            endpoint.setAgentStatus("DISABLED");
            endpoint.setTunnelStatus("OFFLINE");
        } else {
            endpoint.setRevokedAt(null);
        }
        endpointMapper.updateById(endpoint);
        if (!"ENABLED".equals(state)) {
            accessSessionMapper.update(null, new LambdaUpdateWrapper<TunnelAccessSession>()
                .eq(TunnelAccessSession::getEndpointId, endpoint.getId())
                .isNull(TunnelAccessSession::getRevokedAt)
                .set(TunnelAccessSession::getRevokedAt, now)
                .set(TunnelAccessSession::getUpdateTime, now));
        }
        return toView(endpoint);
    }

    TunnelDtos.EndpointView toView(TunnelEndpoint endpoint) {
        TunnelDtos.EndpointView view = new TunnelDtos.EndpointView();
        view.setFeatureEnabled(properties.isEnabled());
        view.setConfigured(true);
        view.setEndpointId(String.valueOf(endpoint.getId()));
        view.setDeviceId(endpoint.getDeviceId());
        view.setDeviceName(endpoint.getDeviceName());
        view.setDesiredState(endpoint.getDesiredState());
        view.setAgentStatus(endpoint.getAgentStatus());
        view.setTunnelStatus(endpoint.getTunnelStatus());
        view.setLocalWebStatus(endpoint.getLocalWebStatus());
        view.setRouteStatus(endpoint.getRouteStatus());
        view.setConfigGeneration(endpoint.getConfigGeneration());
        view.setLastHeartbeatAt(endpoint.getLastHeartbeatAt());
        view.setLastErrorCode(endpoint.getLastErrorCode());
        view.setLastErrorMessage(endpoint.getLastErrorMessage());
        long timeout = Math.max(30, properties.getHeartbeatTimeoutSeconds()) * 1000L;
        view.setAgentOnline(endpoint.getLastHeartbeatAt() != null
            && endpoint.getLastHeartbeatAt().getTime() >= System.currentTimeMillis() - timeout
            && !"REVOKED".equals(endpoint.getDesiredState()));
        return view;
    }

    private TunnelEndpoint requireEndpoint(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            throw new ServiceException("设备业务ID不能为空");
        }
        TunnelEndpoint endpoint = endpointMapper.selectOne(new LambdaQueryWrapper<TunnelEndpoint>()
            .eq(TunnelEndpoint::getDeviceId, deviceId)
            .eq(TunnelEndpoint::getIsDeleted, 0)
            .last("limit 1"));
        if (endpoint == null) {
            throw new ServiceException("该设备尚未配置远程管理");
        }
        return endpoint;
    }

    private int allocatePort() {
        int start = properties.getServerPortStart();
        int end = properties.getServerPortEnd();
        if (start < 1024 || end > 65535 || start > end) {
            throw new ServiceException("隧道服务端口范围配置不正确");
        }
        for (int port = start; port <= end; port++) {
            if (endpointMapper.countByServerBindPort(port) == 0) {
                return port;
            }
        }
        throw new ServiceException("隧道内部端口池已耗尽");
    }

    private String currentTenantId() {
        LoginUser user = LoginHelper.getLoginUser();
        String tenantId = user == null ? null : user.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = TenantContextHolder.getTenantId();
        }
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("当前会话缺少租户上下文");
        }
        return tenantId;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ServiceException("IPC远程管理功能未启用");
        }
        if (!"127.0.0.1".equals(properties.getServerBindHost())
            && !"localhost".equalsIgnoreCase(properties.getServerBindHost())) {
            throw new ServiceException("MVP要求rathole映射端口仅绑定服务器回环地址");
        }
    }

    private int normalizedEnrollmentTtl() {
        return Math.max(60, Math.min(3600, properties.getEnrollmentTtlSeconds()));
    }

    private long nextGeneration(Long current) {
        return current == null ? 1L : current + 1L;
    }
}
