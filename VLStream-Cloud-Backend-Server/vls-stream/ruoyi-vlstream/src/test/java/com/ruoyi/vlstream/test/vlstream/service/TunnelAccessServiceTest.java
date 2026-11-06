/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelAccessSessionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelAccessSession;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.mapping.Environment;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class TunnelAccessServiceTest {
    /** Optional real-MySQL regression, restricted to a disposable loopback test database. */
    @Test
    @EnabledIfEnvironmentVariable(named = "VLS_TUNNEL_TEST_JDBC_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/ipc_tunnel_readonly_test(\\?.*)?")
    void upgradedValidationWorksInEnforcedMySqlReadOnlyTransaction() throws Exception {
        Instant now = Instant.parse("2026-11-04T08:00:00Z");
        DriverManagerDataSource datasource = new DriverManagerDataSource(
            System.getenv("VLS_TUNNEL_TEST_JDBC_URL"), System.getenv("VLS_TUNNEL_TEST_JDBC_USER"),
            System.getenv("VLS_TUNNEL_TEST_JDBC_PASSWORD"));
        try (Connection connection = datasource.getConnection(); Statement ddl = connection.createStatement()) {
            ddl.execute("CREATE TABLE vls_tunnel_access_session (id BIGINT PRIMARY KEY, tenant_id VARCHAR(64), "
                + "session_id VARCHAR(64), endpoint_id BIGINT, device_id VARCHAR(64), opened_at DATETIME(3), "
                + "expires_at DATETIME(3), revoked_at DATETIME(3), update_time DATETIME(3))");
            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO vls_tunnel_access_session VALUES (2, '000000', 'session-1', 1, 'IPC-1', ?, ?, NULL, NULL)")) {
                insert.setTimestamp(1, Timestamp.from(now.minusSeconds(60)));
                insert.setTimestamp(2, Timestamp.from(now.plusSeconds(3600)));
                insert.executeUpdate();
            }
        }
        try {
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setEnvironment(new Environment("isolated-readonly-regression",
                new SpringManagedTransactionFactory(), datasource));
            configuration.addMapper(TunnelAccessSessionMapper.class);
            SqlSessionTemplate sql = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
            TunnelAccessSessionMapper sessions = sql.getMapper(TunnelAccessSessionMapper.class);
            DataSourceTransactionManager transactions = new DataSourceTransactionManager(datasource);
            TransactionTemplate readOnly = new TransactionTemplate(transactions);
            readOnly.setReadOnly(true);
            RuntimeException lockingReadFailure = assertThrows(RuntimeException.class,
                () -> readOnly.execute(status -> sessions.selectBySessionId("session-1")));
            Throwable cause = lockingReadFailure;
            while (!(cause instanceof SQLException) && cause.getCause() != null) cause = cause.getCause();
            assertTrue(cause instanceof SQLException, "the control must fail in MySQL, not in test wiring");
            assertEquals(1792, ((SQLException) cause).getErrorCode(), "locking SELECT is forbidden in READ ONLY");

            VlsTunnelProperties properties = new VlsTunnelProperties();
            properties.setEnabled(true);
            TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
            TunnelEndpoint endpoint = readyEndpoint();
            endpoint.setLastHeartbeatAt(Date.from(now));
            when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);
            TunnelAccessService target = new TunnelAccessService(endpoints, sessions,
                new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
            ProxyFactory proxy = new ProxyFactory(target);
            proxy.setProxyTargetClass(true);
            proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
            TunnelAccessService service = (TunnelAccessService) proxy.getProxy();
            for (int i = 0; i < 3; i++) {
                assertEquals(Date.from(now.plusSeconds(3600)), service.validateSession("session-1").getExpiresAt());
            }
            TunnelAccessSession unchanged = sessions.selectBySessionIdReadOnly("session-1");
            assertEquals(Date.from(now.plusSeconds(3600)), unchanged.getExpiresAt());
            assertEquals(Date.from(now.minusSeconds(60)), unchanged.getOpenedAt());
            assertEquals(null, unchanged.getUpdateTime());
            TransactionTemplate write = new TransactionTemplate(transactions);
            assertEquals("session-1", write.execute(status -> sessions.selectBySessionId("session-1")).getSessionId(),
                "renewal's existing locking read must remain valid in a writable transaction");
        } finally {
            try (Connection connection = datasource.getConnection(); Statement ddl = connection.createStatement()) {
                ddl.execute("DROP TABLE vls_tunnel_access_session");
            }
        }
    }

    @Test
    void upgradedValidationDoesNotRenewSessionOrWriteAnything() {
        Instant now = Instant.parse("2026-10-28T14:00:00Z");
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessService service = new TunnelAccessService(endpoints, sessions,
            new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
        TunnelAccessSession session = openedSession(now);
        Date expires = session.getExpiresAt();
        Date opened = session.getOpenedAt();
        TunnelEndpoint endpoint = readyEndpoint();
        endpoint.setLastHeartbeatAt(Date.from(now));
        when(sessions.selectBySessionIdReadOnly("session-1")).thenReturn(session);
        when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);
        for (int i = 0; i < 3; i++) {
            TunnelDtos.SessionRouteView route = service.validateSession("session-1");
            assertEquals(expires, route.getExpiresAt());
            assertEquals(opened, session.getOpenedAt());
            assertEquals(null, session.getUpdateTime());
        }
        verify(sessions, never()).updateById(any(TunnelAccessSession.class));
        verify(sessions, never()).selectByTokenHash(any());
        verify(sessions, never()).selectBySessionId(any());
    }

    @Test
    void upgradedValidationRejectsExpiredRevokedUnopenedAndMissingSessions() {
        Instant now = Instant.parse("2026-10-28T14:00:00Z");
        for (String state : new String[]{"expired", "revoked", "unopened", "missing"}) {
            VlsTunnelProperties properties = new VlsTunnelProperties();
            properties.setEnabled(true);
            TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
            TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
            TunnelAccessService service = new TunnelAccessService(endpoints, sessions,
                new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
            TunnelAccessSession session = openedSession(now);
            if ("expired".equals(state)) session.setExpiresAt(Date.from(now));
            if ("revoked".equals(state)) session.setRevokedAt(Date.from(now));
            if ("unopened".equals(state)) session.setOpenedAt(null);
            when(sessions.selectBySessionIdReadOnly("session-1")).thenReturn("missing".equals(state) ? null : session);
            assertEquals(401, assertThrows(TunnelApiException.class,
                () -> service.validateSession("session-1")).getStatus(), state);
            verify(sessions, never()).updateById(any(TunnelAccessSession.class));
            verify(endpoints, never()).selectByIdGlobal(any());
        }
    }

    @Test
    void upgradedValidationRejectsChangedOwnershipAndUnavailableDevices() {
        Instant now = Instant.parse("2026-10-28T14:00:00Z");
        for (String state : new String[]{"other-tenant", "other-device", "disabled", "stale-heartbeat",
            "tunnel-offline", "web-unavailable", "route-generation", "feature-disabled"}) {
            VlsTunnelProperties properties = new VlsTunnelProperties();
            properties.setEnabled(!"feature-disabled".equals(state));
            TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
            TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
            TunnelAccessService service = new TunnelAccessService(endpoints, sessions,
                new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
            TunnelEndpoint endpoint = readyEndpoint();
            endpoint.setLastHeartbeatAt(Date.from(now));
            if ("other-tenant".equals(state)) endpoint.setTenantId("another-tenant");
            if ("other-device".equals(state)) endpoint.setDeviceId("another-device");
            if ("disabled".equals(state)) endpoint.setDesiredState("DISABLED");
            if ("stale-heartbeat".equals(state)) endpoint.setLastHeartbeatAt(Date.from(now.minusSeconds(300)));
            if ("tunnel-offline".equals(state)) endpoint.setTunnelStatus("OFFLINE");
            if ("web-unavailable".equals(state)) endpoint.setLocalWebStatus("UNAVAILABLE");
            if ("route-generation".equals(state)) endpoint.setRouteAppliedGeneration(99L);
            when(sessions.selectBySessionIdReadOnly("session-1")).thenReturn(openedSession(now));
            when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);
            int expected = state.startsWith("other-") ? 401 : 503;
            assertEquals(expected, assertThrows(TunnelApiException.class,
                () -> service.validateSession("session-1")).getStatus(), state);
            verify(sessions, never()).updateById(any(TunnelAccessSession.class));
        }
    }

    @Test
    void upgradedValidationControllerRequiresExistingGatewayCredential() {
        Instant now = Instant.parse("2026-10-28T14:00:00Z");
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        properties.setGatewayApiToken("fixture-gateway-012345678901234567890123456789");
        properties.setControlApiToken("fixture-control-012345678901234567890123456789");
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessService access = new TunnelAccessService(endpoints, sessions,
            new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
        com.ruoyi.vlstream.test.vlstream.controller.VlsTunnelInternalController controller =
            new com.ruoyi.vlstream.test.vlstream.controller.VlsTunnelInternalController(
                mock(TunnelControlService.class), access);
        assertEquals(401, controller.validateSession(null, "session-1").getStatusCodeValue());
        assertEquals(401, controller.validateSession(properties.getControlApiToken(), "session-1").getStatusCodeValue());
        verify(sessions, never()).selectBySessionIdReadOnly(any());
        TunnelAccessSession session = openedSession(now);
        TunnelEndpoint endpoint = readyEndpoint();
        endpoint.setLastHeartbeatAt(Date.from(now));
        when(sessions.selectBySessionIdReadOnly("session-1")).thenReturn(session);
        when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);
        assertEquals(200, controller.validateSession(properties.getGatewayApiToken(), "session-1").getStatusCodeValue());
        verify(sessions, never()).updateById(any(TunnelAccessSession.class));
    }

    @Test
    void gatewayExpiryHasExplicitUtcOffsetDespiteLocalJacksonDefaults() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        TunnelDtos.SessionRouteView route = new TunnelDtos.SessionRouteView();
        route.setExpiresAt(Date.from(Instant.parse("2026-09-22T20:00:00Z")));
        assertEquals("2026-09-22T20:00:00.000Z", mapper.readTree(mapper.writeValueAsString(route)).get("expiresAt").asText());
    }

    @Test
    void activityRenewsOpenedSessionForAnotherTenHoursWithoutReopeningToken() {
        Instant now = Instant.parse("2026-09-22T10:00:00Z");
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessService service = new TunnelAccessService(endpoints, sessions,
            new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
        TunnelAccessSession session = openedSession(now);
        Date openedAt = session.getOpenedAt();
        TunnelEndpoint endpoint = readyEndpoint();
        endpoint.setLastHeartbeatAt(Date.from(now));
        when(sessions.selectBySessionId("session-1")).thenReturn(session);
        when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);

        TunnelDtos.SessionRouteView route = service.renewSession("session-1");

        assertEquals(Date.from(now.plusSeconds(36000)), route.getExpiresAt());
        assertEquals(openedAt, session.getOpenedAt());
        assertEquals(Date.from(now), session.getUpdateTime());
        verify(sessions).updateById(session);
        verify(sessions).selectBySessionId("session-1");
        verify(sessions, never()).selectBySessionIdReadOnly(any());
        verify(sessions, never()).selectByTokenHash(any());
    }

    @Test
    void invalidSessionsCannotBeRevivedByRenewal() {
        Instant now = Instant.parse("2026-09-22T10:00:00Z");
        for (String state : new String[]{"expired", "revoked", "unopened"}) {
            VlsTunnelProperties properties = new VlsTunnelProperties();
            properties.setEnabled(true);
            TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
            TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
            TunnelAccessService service = new TunnelAccessService(endpoints, sessions,
                new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
            TunnelAccessSession session = openedSession(now);
            if ("expired".equals(state)) session.setExpiresAt(Date.from(now));
            if ("revoked".equals(state)) session.setRevokedAt(Date.from(now.minusSeconds(1)));
            if ("unopened".equals(state)) session.setOpenedAt(null);
            when(sessions.selectBySessionId("session-1")).thenReturn(session);

            TunnelApiException exception = assertThrows(TunnelApiException.class,
                () -> service.renewSession("session-1"));
            assertEquals(401, exception.getStatus(), state);
            verify(sessions, never()).updateById(any(TunnelAccessSession.class));
            verify(endpoints, never()).selectByIdGlobal(any());
        }
    }

    @Test
    void renewalChecksOwnershipAndEndpointReadiness() {
        Instant now = Instant.parse("2026-09-22T10:00:00Z");
        for (String state : new String[]{"other-tenant", "disabled", "stale-heartbeat", "feature-disabled"}) {
            VlsTunnelProperties properties = new VlsTunnelProperties();
            properties.setEnabled(!"feature-disabled".equals(state));
            TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
            TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
            TunnelAccessService service = new TunnelAccessService(endpoints, sessions,
                new TunnelTokenService(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
            TunnelEndpoint endpoint = readyEndpoint();
            endpoint.setLastHeartbeatAt(Date.from(now));
            if ("other-tenant".equals(state)) endpoint.setTenantId("tenant-b");
            if ("disabled".equals(state)) endpoint.setDesiredState("DISABLED");
            if ("stale-heartbeat".equals(state)) endpoint.setLastHeartbeatAt(Date.from(now.minusSeconds(300)));
            when(sessions.selectBySessionId("session-1")).thenReturn(openedSession(now));
            when(endpoints.selectByIdGlobal(1L)).thenReturn(endpoint);

            TunnelApiException exception = assertThrows(TunnelApiException.class,
                () -> service.renewSession("session-1"));
            assertEquals("other-tenant".equals(state) ? 401 : 503, exception.getStatus(), state);
            verify(sessions, never()).updateById(any(TunnelAccessSession.class));
        }
    }

    private TunnelAccessSession openedSession(Instant now) {
        TunnelAccessSession session = new TunnelAccessSession();
        session.setId(2L);
        session.setSessionId("session-1");
        session.setEndpointId(1L);
        session.setTenantId("000000");
        session.setDeviceId("IPC-1");
        session.setCreateTime(Date.from(now.minusSeconds(9 * 3600)));
        session.setOpenedAt(session.getCreateTime());
        session.setExpiresAt(Date.from(now.plusSeconds(3600)));
        return session;
    }

    @Test
    void newAccessSessionDefaultsToTenHoursFromIssueTime() {
        assertIssuedSessionTtl(new VlsTunnelProperties(), 36000);
    }

    @Test
    void accessSessionSupportsConfiguredTtlWithinTenHours() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setAccessSessionTtlSeconds(7200);
        assertIssuedSessionTtl(properties, 7200);
        properties.setAccessSessionTtlSeconds(Integer.MAX_VALUE);
        assertIssuedSessionTtl(properties, 36000);
        properties.setAccessSessionTtlSeconds(0);
        assertIssuedSessionTtl(properties, 60);
    }

    @Test
    void expiredUnopenedSessionCannotBeRedeemed() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        TunnelTokenService tokens = new TunnelTokenService(properties);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        TunnelAccessService service = new TunnelAccessService(
            mock(TunnelEndpointMapper.class), sessions, tokens, properties);
        TunnelAccessSession session = new TunnelAccessSession();
        session.setCreateTime(new Date(System.currentTimeMillis() - 36000_001L));
        session.setExpiresAt(new Date(System.currentTimeMillis() - 1L));
        when(sessions.selectByTokenHash(tokens.hash("expired-token"))).thenReturn(session);

        TunnelApiException exception = assertThrows(TunnelApiException.class,
            () -> service.resolveSession("expired-token"));
        assertEquals(401, exception.getStatus());
    }

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

    private void assertIssuedSessionTtl(VlsTunnelProperties properties, int expectedSeconds) {
        properties.setEnabled(true);
        properties.setGatewayBaseUrl("https://{sessionId}.ipc.example.com");
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelAccessSessionMapper sessions = mock(TunnelAccessSessionMapper.class);
        when(endpoints.selectOne(any())).thenReturn(readyEndpoint());
        TunnelAccessService service = new TunnelAccessService(
            endpoints, sessions, new TunnelTokenService(properties), properties);

        TunnelDtos.AccessSessionView view = service.createSession("IPC-1");
        ArgumentCaptor<TunnelAccessSession> saved = ArgumentCaptor.forClass(TunnelAccessSession.class);
        verify(sessions).insert(saved.capture());
        assertEquals(expectedSeconds * 1000L,
            saved.getValue().getExpiresAt().getTime() - saved.getValue().getCreateTime().getTime());
        assertEquals(saved.getValue().getExpiresAt(), view.getExpiresAt());
    }

    private TunnelEndpoint readyEndpoint() {
        TunnelEndpoint endpoint = new TunnelEndpoint();
        endpoint.setId(1L);
        endpoint.setTenantId("000000");
        endpoint.setDeviceId("IPC-1");
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
