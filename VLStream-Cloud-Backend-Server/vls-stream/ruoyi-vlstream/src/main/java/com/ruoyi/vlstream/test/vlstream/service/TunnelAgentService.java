/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEnrollmentMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEnrollment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class TunnelAgentService {
    private static final Set<String> AGENT_STATUSES = new HashSet<String>(Arrays.asList(
        "REGISTERED", "CONNECTING", "ONLINE", "DEGRADED", "CONFIG_INVALID", "AUTH_FAILED"
    ));
    private static final Set<String> TUNNEL_STATUSES = new HashSet<String>(Arrays.asList(
        "CONNECTING", "ONLINE", "OFFLINE", "DEGRADED"
    ));
    private static final Set<String> LOCAL_WEB_STATUSES = new HashSet<String>(Arrays.asList(
        "AVAILABLE", "UNAVAILABLE", "UNKNOWN"
    ));
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)(token|authorization|cookie|password|secret)\\s*[:=]\\s*[^\\s,;]+"
    );

    private final TunnelEndpointMapper endpointMapper;
    private final TunnelEnrollmentMapper enrollmentMapper;
    private final TunnelTokenService tokenService;
    private final VlsTunnelProperties properties;

    public TunnelAgentService(TunnelEndpointMapper endpointMapper,
                              TunnelEnrollmentMapper enrollmentMapper,
                              TunnelTokenService tokenService,
                              VlsTunnelProperties properties) {
        this.endpointMapper = endpointMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Transactional(rollbackFor = Exception.class)
    public TunnelDtos.AgentConfigView register(TunnelDtos.AgentRegisterRequest request) {
        requireAgentConfiguration();
        validateAgentInstanceId(request.getAgentInstanceId());
        if (!"http".equalsIgnoreCase(StringUtils.trimToEmpty(request.getLocalWebScheme()))) {
            throw new TunnelApiException(400, "MVP仅支持IPC本机HTTP管理后台");
        }
        String codeHash = tokenService.hash(request.getEnrollmentCode());
        TunnelEnrollment enrollment = enrollmentMapper.selectByCodeHashForUpdate(codeHash);
        Date now = new Date();
        if (enrollment == null || enrollment.getExpiresAt() == null
            || !enrollment.getExpiresAt().after(now)) {
            throw new TunnelApiException(401, "激活码无效或已过期");
        }
        if (!StringUtils.equals(enrollment.getDeviceId(), StringUtils.trim(request.getDeviceId()))) {
            throw new TunnelApiException(403, "激活码与设备不匹配");
        }
        if (enrollment.getConsumedAt() != null) {
            if (StringUtils.equals(enrollment.getConsumedAgentInstanceId(), request.getAgentInstanceId())) {
                throw new TunnelApiException(409, "Agent已注册；请使用已保存的Agent Token，丢失时由平台重新签发激活码");
            }
            throw new TunnelApiException(409, "激活码已被其他Agent使用");
        }

        TunnelEndpoint endpoint = endpointMapper.selectByIdGlobal(enrollment.getEndpointId());
        if (endpoint == null || !StringUtils.equals(endpoint.getDeviceId(), enrollment.getDeviceId())) {
            throw new TunnelApiException(404, "远程管理端点不存在");
        }
        if ("REVOKED".equals(endpoint.getDesiredState())) {
            throw new TunnelApiException(410, "远程管理端点已吊销");
        }

        String agentToken = tokenService.randomToken(32);
        endpoint.setAgentInstanceId(request.getAgentInstanceId());
        endpoint.setAgentTokenHash(tokenService.hash(agentToken));
        endpoint.setAgentVersion(StringUtils.trim(request.getAgentVersion()));
        endpoint.setArchitecture(StringUtils.trim(request.getArchitecture()));
        endpoint.setFirmwareVersion(StringUtils.trimToNull(request.getFirmwareVersion()));
        endpoint.setInitSystem(StringUtils.trimToNull(request.getInitSystem()));
        endpoint.setLocalWebScheme("http");
        endpoint.setLocalWebPort(request.getLocalWebPort());
        endpoint.setAgentStatus("REGISTERED");
        endpoint.setTunnelStatus("OFFLINE");
        endpoint.setLocalWebStatus("UNKNOWN");
        endpoint.setLastErrorCode(null);
        endpoint.setLastErrorMessage(null);
        endpoint.setUpdateTime(now);
        TunnelTenantScope.run(endpoint.getTenantId(), new Runnable() {
            @Override
            public void run() {
                endpointMapper.updateById(endpoint);
            }
        });

        enrollment.setConsumedAt(now);
        enrollment.setConsumedAgentInstanceId(request.getAgentInstanceId());
        enrollment.setUpdateTime(now);
        TunnelTenantScope.run(enrollment.getTenantId(), new Runnable() {
            @Override
            public void run() {
                enrollmentMapper.updateById(enrollment);
            }
        });
        return toConfig(endpoint, agentToken);
    }

    public TunnelDtos.AgentConfigView config(String rawAgentToken) {
        TunnelEndpoint endpoint = authenticate(rawAgentToken);
        return toConfig(endpoint, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public TunnelDtos.HeartbeatView heartbeat(String rawAgentToken,
                                               TunnelDtos.AgentHeartbeatRequest request) {
        TunnelEndpoint endpoint = authenticate(rawAgentToken);
        if (!StringUtils.equals(endpoint.getAgentInstanceId(), request.getAgentInstanceId())) {
            throw new TunnelApiException(409, "Agent实例与注册记录不一致");
        }
        String agentStatus = normalize(request.getAgentStatus());
        String tunnelStatus = normalize(request.getTunnelStatus());
        String localWebStatus = normalize(request.getLocalWebStatus());
        if (!AGENT_STATUSES.contains(agentStatus)
            || !TUNNEL_STATUSES.contains(tunnelStatus)
            || !LOCAL_WEB_STATUSES.contains(localWebStatus)) {
            throw new TunnelApiException(400, "Agent、隧道或本地Web状态值不合法");
        }
        Date now = new Date();
        endpoint.setAgentStatus(agentStatus);
        endpoint.setTunnelStatus(tunnelStatus);
        endpoint.setLocalWebStatus(localWebStatus);
        endpoint.setLastHeartbeatAt(now);
        endpoint.setLastReportedAt(parseReportedAt(request.getReportedAt()));
        endpoint.setLastErrorCode(StringUtils.abbreviate(StringUtils.trimToNull(request.getLastErrorCode()), 64));
        endpoint.setLastErrorMessage(sanitizeError(request.getLastErrorMessage()));
        endpoint.setUpdateTime(now);
        TunnelTenantScope.run(endpoint.getTenantId(), new Runnable() {
            @Override
            public void run() {
                endpointMapper.updateById(endpoint);
            }
        });

        TunnelDtos.HeartbeatView view = new TunnelDtos.HeartbeatView();
        view.setDesiredState(endpoint.getDesiredState());
        view.setConfigGeneration(endpoint.getConfigGeneration());
        view.setConfigChanged(!endpoint.getConfigGeneration().equals(request.getConfigGeneration()));
        view.setNextHeartbeatSeconds(normalizedHeartbeatInterval());
        return view;
    }

    TunnelEndpoint authenticate(String rawAgentToken) {
        if (StringUtils.isBlank(rawAgentToken)) {
            throw new TunnelApiException(401, "缺少Agent Token");
        }
        TunnelEndpoint endpoint = endpointMapper.selectByAgentTokenHash(tokenService.hash(rawAgentToken));
        if (endpoint == null) {
            throw new TunnelApiException(401, "Agent Token无效");
        }
        if ("REVOKED".equals(endpoint.getDesiredState())) {
            throw new TunnelApiException(410, "远程管理端点已吊销");
        }
        return endpoint;
    }

    TunnelDtos.AgentConfigView toConfig(TunnelEndpoint endpoint, String agentToken) {
        TunnelDtos.TunnelConfigView tunnel = new TunnelDtos.TunnelConfigView();
        tunnel.setRemoteAddr(properties.getRemoteAddress());
        tunnel.setServiceName(endpoint.getServiceName());
        tunnel.setServiceToken(tokenService.deriveServiceToken(endpoint.getId(), endpoint.getConfigGeneration()));
        tunnel.setTransport(properties.getTransport());
        tunnel.setRemotePublicKey(properties.getNoiseRemotePublicKey());
        tunnel.setLocalAddr("127.0.0.1:" + endpoint.getLocalWebPort());

        TunnelDtos.AgentConfigView view = new TunnelDtos.AgentConfigView();
        view.setAgentId(String.valueOf(endpoint.getId()));
        view.setAgentToken(agentToken);
        view.setConfigGeneration(endpoint.getConfigGeneration());
        view.setDesiredState(endpoint.getDesiredState());
        view.setHeartbeatIntervalSeconds(normalizedHeartbeatInterval());
        view.setTunnel(tunnel);
        return view;
    }

    private void requireAgentConfiguration() {
        if (!properties.isEnabled()) {
            throw new TunnelApiException(503, "IPC远程管理功能未启用");
        }
        if (StringUtils.isBlank(properties.getRemoteAddress())) {
            throw new TunnelApiException(503, "rathole远端地址未配置");
        }
        if (!"noise".equalsIgnoreCase(properties.getTransport())) {
            throw new TunnelApiException(503, "MVP只允许Noise加密隧道");
        }
        if (StringUtils.isBlank(properties.getNoiseRemotePublicKey())) {
            throw new TunnelApiException(503, "Noise服务端公钥未配置");
        }
        tokenService.deriveServiceToken(0L, 0L);
    }

    private void validateAgentInstanceId(String value) {
        try {
            UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new TunnelApiException(400, "agentInstanceId必须是UUID");
        }
    }

    private String normalize(String value) {
        return StringUtils.upperCase(StringUtils.trimToEmpty(value), Locale.ROOT);
    }

    private String sanitizeError(String value) {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return StringUtils.abbreviate(SECRET_PATTERN.matcher(normalized).replaceAll("$1=[REDACTED]"), 255);
    }

    private Date parseReportedAt(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Date.from(Instant.parse(StringUtils.trim(value)));
        } catch (RuntimeException exception) {
            throw new TunnelApiException(400, "reportedAt必须是UTC ISO-8601时间");
        }
    }

    private int normalizedHeartbeatInterval() {
        return Math.max(10, Math.min(300, properties.getHeartbeatIntervalSeconds()));
    }
}
