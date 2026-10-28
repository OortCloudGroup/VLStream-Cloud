package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.web.controller.compat.BladeTokenSessionService;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysUser;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class PlatformSessionValidationServiceTest {
    private final PlatformTenantClient client = mock(PlatformTenantClient.class);
    private final PlatformTenantSessionStore sessions = mock(PlatformTenantSessionStore.class);
    private final MultiTenantAuthService service = new MultiTenantAuthService(client,
        mock(MultiTenantShadowUserService.class), sessions, mock(BladeTokenUserStore.class),
        mock(BladeTokenSessionService.class), mock(SysLoginService.class), new TokenProperties(), -1L);

    @Test void missingBindingIsDistinctFromAnUpstreamFailure() {
        PlatformSessionValidationException error = assertThrows(PlatformSessionValidationException.class,
            () -> service.validatePlatformSession("fixture-local"));
        assertEquals(PlatformSessionValidationException.Reason.SESSION_NOT_BOUND,error.getReason());
        verifyNoInteractions(client);
    }

    @Test void identityChangesIdentifyTheFieldWithoutExposingEitherIdentity() {
        PlatformTenantSession session = session(); when(sessions.get("fixture-local")).thenReturn(session);
        PlatformIdentity changedUser = identity("different-user","tenant-a");
        when(client.verifyToken("fixture-platform",session.getGatewayHeaders())).thenReturn(changedUser);
        assertEquals(PlatformSessionValidationException.Reason.PLATFORM_USER_CHANGED,
            assertThrows(PlatformSessionValidationException.class,()->service.validatePlatformSession("fixture-local")).getReason());
        when(client.verifyToken("fixture-platform",session.getGatewayHeaders())).thenReturn(identity("user-a","different-tenant"));
        assertEquals(PlatformSessionValidationException.Reason.PLATFORM_TENANT_CHANGED,
            assertThrows(PlatformSessionValidationException.class,()->service.validatePlatformSession("fixture-local")).getReason());
        verify(sessions,never()).remove(anyString());
    }

    @Test void upstreamOutagePreservesBindingAndItsOriginalClassification() {
        PlatformTenantSession session = session();when(sessions.get("fixture-local")).thenReturn(session);
        PlatformTenantCallException outage = new PlatformTenantCallException(PlatformTenantCallException.Kind.TRANSPORT,null,null,null);
        when(client.verifyToken("fixture-platform",session.getGatewayHeaders())).thenThrow(outage);
        assertSame(outage,assertThrows(PlatformTenantCallException.class,()->service.validatePlatformSession("fixture-local")));
        verify(sessions,never()).remove(anyString());
    }

    @Test void matchingVerifiedIdentityStillReturnsTheBoundTenant() {
        PlatformTenantSession session = session();when(sessions.get("fixture-local")).thenReturn(session);
        when(client.verifyToken("fixture-platform",session.getGatewayHeaders())).thenReturn(identity("user-a","tenant-a"));
        assertEquals("tenant-a",service.validatePlatformSession("fixture-local").get("tenantId"));
        verify(sessions,never()).remove(anyString());
    }

    @Test void exchangeAndTenantSwitchRevalidateTheirOwnBoundPlatformCredential() {
        MultiTenantShadowUserService shadow = mock(MultiTenantShadowUserService.class);
        SysLoginService login = mock(SysLoginService.class);
        MultiTenantAuthService lifecycle = new MultiTenantAuthService(client, shadow, sessions,
            mock(BladeTokenUserStore.class), mock(BladeTokenSessionService.class), login,
            new TokenProperties(), 3600L);
        Map<String, PlatformTenantSession> saved = new HashMap<>();
        doAnswer(call -> {saved.put(call.getArgument(0), call.getArgument(1)); return null;})
            .when(sessions).put(anyString(), any(PlatformTenantSession.class), eq(3600L));
        when(sessions.get(anyString())).thenAnswer(call -> saved.get(call.getArgument(0)));
        doAnswer(call -> {saved.remove(call.getArgument(0)); return null;}).when(sessions).remove(anyString());
        PlatformGatewayHeaders gateway = new PlatformGatewayHeaders("app", "fixture-app", "fixture-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(client.resolveGatewayHeaders(request)).thenReturn(gateway);
        when(client.verifyToken("fixture-platform-a", gateway)).thenReturn(identity("user-a", "tenant-a"));
        when(client.verifyToken("fixture-platform-b", gateway)).thenReturn(identity("user-a", "tenant-b"));
        PlatformTenant tenantA = new PlatformTenant(); tenantA.setTenantId("tenant-a");
        PlatformTenant tenantB = new PlatformTenant(); tenantB.setTenantId("tenant-b");
        when(client.getUserTenants("fixture-platform-a", gateway)).thenReturn(Arrays.asList(tenantA, tenantB));
        when(client.selectTenant("fixture-platform-a", "tenant-b", gateway)).thenReturn("fixture-platform-b");
        when(shadow.loadOrCreate(any(PlatformIdentity.class), any())).thenReturn(new SysUser());
        when(login.loginPlatformUser(any())).thenReturn("fixture-local-a", "fixture-local-b");

        lifecycle.exchange("fixture-platform-a", "tenant-a", request);
        assertEquals("fixture-platform-a", saved.get("fixture-local-a").getPlatformAccessToken());
        assertSame(gateway, saved.get("fixture-local-a").getGatewayHeaders());
        assertEquals("tenant-a", lifecycle.validatePlatformSession("fixture-local-a").get("tenantId"));
        lifecycle.switchTenant("fixture-local-a", "tenant-b");
        assertFalse(saved.containsKey("fixture-local-a"));
        assertEquals("fixture-platform-b", saved.get("fixture-local-b").getPlatformAccessToken());
        assertEquals("tenant-b", lifecycle.validatePlatformSession("fixture-local-b").get("tenantId"));
        verify(client, times(2)).verifyToken("fixture-platform-a", gateway);
        verify(client, times(2)).verifyToken("fixture-platform-b", gateway);
        verify(client, never()).verifyToken(eq("fixture-local-a"), any());
        verify(client, never()).verifyToken(eq("fixture-local-b"), any());
    }

    private PlatformTenantSession session() {
        PlatformTenantSession session = new PlatformTenantSession(); session.setPlatformAccessToken("fixture-platform");
        session.setPlatformUserId("user-a");session.setTenantId("tenant-a");
        session.setGatewayHeaders(new PlatformGatewayHeaders("app","fixture-app","fixture-secret"));return session;
    }
    private PlatformIdentity identity(String user,String tenant) {
        PlatformIdentity identity = new PlatformIdentity();identity.setUserId(user);identity.setTenantId(tenant);return identity;
    }
}
