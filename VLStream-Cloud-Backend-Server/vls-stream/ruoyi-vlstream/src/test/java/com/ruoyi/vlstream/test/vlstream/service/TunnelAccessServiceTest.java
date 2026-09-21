/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelAccessSessionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelAccessSession;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class TunnelAccessServiceTest {
    @Test
    void gatewayResolveReturnsOnlyLoopbackRouteForReadyEndpoint() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        properties.setServerBindHost("127.0.0.1");
        properties.setServiceSigningSecret("0123456789abcdef0123456789abcdef");
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelAccessService service = new TunnelAccessService(endpoints, sessions, tokens, properties);

        String rawToken = "short-lived-browser-token";
        TunnelAccessSession session = new TunnelAccessSession();
        session.setId(2L);
        session.setTenantId("000000");
        session.setSessionId("session-1");
        session.setEndpointId(1L);
        session.setAccessTokenHash(tokens.hash(rawToken));
        session.setExpiresAt(new Date(System.currentTimeMillis() + 60000L));
        TunnelEndpoint endpoint = readyEndpoint();
        when(sessions.selectByTokenHash(tokens.hash(rawToken))).thenReturn(session);
        when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);
        when(sessions.updateById(any(TunnelAccessSession.class))).thenReturn(1);

        TunnelDtos.SessionRouteView route = service.resolveSession(rawToken);
        assertEquals("127.0.0.1", route.getUpstreamHost());
        assertEquals(Integer.valueOf(61000), route.getUpstreamPort());
        verify(sessions).updateById(session);
    }

    @Test
    void gatewayResolveFailsBeforeRatholeRouteIsApplied() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelAccessService service = new TunnelAccessService(endpoints, sessions, tokens, properties);

        String rawToken = "pending-route-token";
        TunnelAccessSession session = new TunnelAccessSession();
        session.setEndpointId(1L);
        session.setAccessTokenHash(tokens.hash(rawToken));
        session.setExpiresAt(new Date(System.currentTimeMillis() + 60000L));
        TunnelEndpoint endpoint = readyEndpoint();
        endpoint.setRouteStatus("PENDING");
        when(sessions.selectByTokenHash(tokens.hash(rawToken))).thenReturn(session);
        when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);

        TunnelApiException exception = assertThrows(TunnelApiException.class,
            () -> service.resolveSession(rawToken));
        assertEquals(503, exception.getStatus());
    }

    @Test
    void bootstrapTokenCannotBeResolvedTwice() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelAccessService service = new TunnelAccessService(endpoints, sessions, tokens, properties);

        String rawToken = "already-consumed-token";
        TunnelAccessSession session = new TunnelAccessSession();
        session.setEndpointId(1L);
        session.setAccessTokenHash(tokens.hash(rawToken));
        session.setExpiresAt(new Date(System.currentTimeMillis() + 60000L));
        session.setOpenedAt(new Date());
        when(sessions.selectByTokenHash(tokens.hash(rawToken))).thenReturn(session);

        TunnelApiException exception = assertThrows(TunnelApiException.class,
            () -> service.resolveSession(rawToken));
        assertEquals(401, exception.getStatus());
    }

    @Test
    void accessUrlUsesOneWildcardSubdomainPerSession() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setGatewayBaseUrl("https://{sessionId}.ipc.example.com");
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelAccessService service = new TunnelAccessService(
            mock(TunnelEndpointMapper.class), mock(TunnelAccessSessionMapper.class), tokens, properties);

        String url = service.buildAccessUrl("session-123", "secret/token");
        assertTrue(url.startsWith("https://session-123.ipc.example.com/s/"));
        assertTrue(url.endsWith("secret%2Ftoken"));
    }

    private TunnelEndpoint readyEndpoint() {
        TunnelEndpoint endpoint = new TunnelEndpoint();
        endpoint.setId(1L);
        endpoint.setTenantId("000000");
        endpoint.setDesiredState("ENABLED");
        endpoint.setConfigGeneration(1L);
        endpoint.setRouteAppliedGeneration(1L);
        endpoint.setRouteStatus("APPLIED");
        endpoint.setTunnelStatus("ONLINE");
        endpoint.setLocalWebStatus("AVAILABLE");
        endpoint.setLastHeartbeatAt(new Date());
        endpoint.setServerBindPort(61000);
        return endpoint;
    }
}
