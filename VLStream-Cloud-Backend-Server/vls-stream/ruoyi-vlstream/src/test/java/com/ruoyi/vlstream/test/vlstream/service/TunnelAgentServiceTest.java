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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class TunnelAgentServiceTest {
    @Test
    void enrollmentBindsAgentAndStoresOnlyAgentTokenHash() {
        VlsTunnelProperties properties = properties();
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelEnrollmentMapper enrollments = mock(TunnelEnrollmentMapper.class);
        TunnelAgentService service = new TunnelAgentService(endpoints, enrollments, tokens, properties);

        String enrollmentCode = "one-time-enrollment-code";
        TunnelEnrollment enrollment = enrollment(tokens.hash(enrollmentCode));
        TunnelEndpoint endpoint = endpoint();
        when(enrollments.selectByCodeHashForUpdate(tokens.hash(enrollmentCode))).thenReturn(enrollment);
        when(endpoints.selectByIdGlobal(endpoint.getId())).thenReturn(endpoint);
        when(endpoints.updateById(any(TunnelEndpoint.class))).thenReturn(1);
        when(enrollments.updateById(any(TunnelEnrollment.class))).thenReturn(1);

        TunnelDtos.AgentRegisterRequest request = registerRequest(enrollmentCode);
        TunnelDtos.AgentConfigView result = service.register(request);

        assertNotNull(result.getAgentToken());
        assertFalse(result.getAgentToken().equals(endpoint.getAgentTokenHash()));
        assertEquals(tokens.hash(result.getAgentToken()), endpoint.getAgentTokenHash());
        assertEquals("noise", result.getTunnel().getTransport());
        assertEquals("127.0.0.1:80", result.getTunnel().getLocalAddr());
        assertNotNull(enrollment.getConsumedAt());
        verify(endpoints).updateById(endpoint);
        verify(enrollments).updateById(enrollment);
    }

    @Test
    void consumedEnrollmentCannotBindDifferentAgentInstance() {
        VlsTunnelProperties properties = properties();
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelEnrollmentMapper enrollments = mock(TunnelEnrollmentMapper.class);
        TunnelAgentService service = new TunnelAgentService(endpoints, enrollments, tokens, properties);

        String code = "already-used-code";
        TunnelEnrollment enrollment = enrollment(tokens.hash(code));
        enrollment.setConsumedAt(new Date());
        enrollment.setConsumedAgentInstanceId(UUID.randomUUID().toString());
        when(enrollments.selectByCodeHashForUpdate(tokens.hash(code))).thenReturn(enrollment);

        TunnelApiException exception = assertThrows(TunnelApiException.class,
            () -> service.register(registerRequest(code)));
        assertEquals(409, exception.getStatus());
    }

    @Test
    void consumedEnrollmentDoesNotRotateTokenForSameAgentRetry() {
        VlsTunnelProperties properties = properties();
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelEnrollmentMapper enrollments = mock(TunnelEnrollmentMapper.class);
        TunnelAgentService service = new TunnelAgentService(endpoints, enrollments, tokens, properties);

        String code = "consumed-by-same-agent";
        TunnelDtos.AgentRegisterRequest request = registerRequest(code);
        TunnelEnrollment enrollment = enrollment(tokens.hash(code));
        enrollment.setConsumedAt(new Date());
        enrollment.setConsumedAgentInstanceId(request.getAgentInstanceId());
        when(enrollments.selectByCodeHashForUpdate(tokens.hash(code))).thenReturn(enrollment);

        TunnelApiException exception = assertThrows(TunnelApiException.class,
            () -> service.register(request));
        assertEquals(409, exception.getStatus());
    }

    @Test
    void heartbeatAcceptsNanosecondIsoTimestamp() {
        VlsTunnelProperties properties = properties();
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelEnrollmentMapper enrollments = mock(TunnelEnrollmentMapper.class);
        TunnelAgentService service = new TunnelAgentService(endpoints, enrollments, tokens, properties);
        TunnelEndpoint endpoint = endpoint();
        endpoint.setAgentInstanceId("b6e0f11c-8c14-4be0-91ac-303e8e6bf064");
        endpoint.setAgentTokenHash(tokens.hash("agent-token"));
        when(endpoints.selectByAgentTokenHash(endpoint.getAgentTokenHash())).thenReturn(endpoint);
        when(endpoints.updateById(any(TunnelEndpoint.class))).thenReturn(1);

        TunnelDtos.AgentHeartbeatRequest request = new TunnelDtos.AgentHeartbeatRequest();
        request.setAgentInstanceId(endpoint.getAgentInstanceId());
        request.setConfigGeneration(1L);
        request.setAgentStatus("ONLINE");
        request.setTunnelStatus("ONLINE");
        request.setLocalWebStatus("AVAILABLE");
        request.setReportedAt("2026-09-20T09:59:20.6871376Z");

        TunnelDtos.HeartbeatView result = service.heartbeat("agent-token", request);
        assertEquals("ENABLED", result.getDesiredState());
        assertNotNull(endpoint.getLastReportedAt());
        verify(endpoints).updateById(endpoint);
    }

    private VlsTunnelProperties properties() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        properties.setRemoteAddress("tunnel.example.com:2333");
        properties.setTransport("noise");
        properties.setNoiseRemotePublicKey("public-key");
        properties.setServiceSigningSecret("0123456789abcdef0123456789abcdef");
        return properties;
    }

    private TunnelEnrollment enrollment(String codeHash) {
        TunnelEnrollment enrollment = new TunnelEnrollment();
        enrollment.setId(2L);
        enrollment.setTenantId("000000");
        enrollment.setEndpointId(1L);
        enrollment.setDeviceId("IPC001");
        enrollment.setCodeHash(codeHash);
        enrollment.setExpiresAt(new Date(System.currentTimeMillis() + 60000L));
        return enrollment;
    }

    private TunnelEndpoint endpoint() {
        TunnelEndpoint endpoint = new TunnelEndpoint();
        endpoint.setId(1L);
        endpoint.setTenantId("000000");
        endpoint.setDeviceId("IPC001");
        endpoint.setServiceName("ipc_1");
        endpoint.setConfigGeneration(1L);
        endpoint.setDesiredState("ENABLED");
        endpoint.setLocalWebPort(80);
        return endpoint;
    }

    private TunnelDtos.AgentRegisterRequest registerRequest(String code) {
        TunnelDtos.AgentRegisterRequest request = new TunnelDtos.AgentRegisterRequest();
        request.setDeviceId("IPC001");
        request.setEnrollmentCode(code);
        request.setAgentInstanceId(UUID.randomUUID().toString());
        request.setAgentVersion("1.0.0");
        request.setArchitecture("aarch64");
        request.setFirmwareVersion("v1");
        request.setInitSystem("busybox-init");
        request.setLocalWebScheme("http");
        request.setLocalWebPort(80);
        return request;
    }
}
