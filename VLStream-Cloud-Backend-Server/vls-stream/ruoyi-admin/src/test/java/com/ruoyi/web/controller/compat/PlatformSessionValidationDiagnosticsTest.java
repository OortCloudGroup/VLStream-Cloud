package com.ruoyi.web.controller.compat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.vlstream.test.compat.BladeResult;
import com.ruoyi.web.controller.compat.tenant.MultiTenantAuthService;
import com.ruoyi.web.controller.compat.tenant.PlatformSessionValidationException;
import com.ruoyi.web.controller.compat.tenant.PlatformTenantCallException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class PlatformSessionValidationDiagnosticsTest {
    @Test void failuresRetainTheirReasonAndNeverLogCredentialMessages() {
        Logger logger = (Logger) LoggerFactory.getLogger(SsoCompatController.class);
        ListAppender<ILoggingEvent> capture = new ListAppender<>(); capture.start(); logger.addAppender(capture);
        try {
            for (PlatformSessionValidationException.Reason reason : PlatformSessionValidationException.Reason.values()) {
                BladeResult<Map<String,Object>> result = validate(new PlatformSessionValidationException(reason));
                assertEquals(reason == PlatformSessionValidationException.Reason.SESSION_NOT_BOUND ? 503 : 401, result.getCode());
                assertTrace(result);
                assertTrue(capture.list.get(capture.list.size()-1).getFormattedMessage().contains(reason.name()));
            }
            assertEquals(4004, validate(new PlatformTenantCallException(PlatformTenantCallException.Kind.BUSINESS_CODE,200,4004,null)).getCode());
            assertEquals(403, validate(new PlatformTenantCallException(PlatformTenantCallException.Kind.HTTP_STATUS,403,null,null)).getCode());
            assertEquals(503, validate(new PlatformTenantCallException(PlatformTenantCallException.Kind.TRANSPORT,null,null,
                new IllegalStateException("fixture-secret fixture-token"))).getCode());
            assertEquals(502, validate(new PlatformTenantCallException(PlatformTenantCallException.Kind.RESPONSE_FORMAT,200,null,null)).getCode());
            BladeResult<Map<String,Object>> unexpected = validate(new IllegalArgumentException("fixture-secret fixture-token"));
            assertEquals(500, unexpected.getCode()); assertTrace(unexpected);
            assertFalse(unexpected.getMsg().contains("已失效"));
            for (ILoggingEvent event : capture.list) {
                assertFalse(event.getFormattedMessage().contains("fixture-secret"));
                assertFalse(event.getFormattedMessage().contains("fixture-token"));
                assertNull(event.getThrowableProxy(), "Only safe structured exception metadata may be logged");
            }
        } finally { logger.detachAppender(capture); capture.stop(); }
    }

    @Test void missingLocalTokenIsExplicitlyUnauthenticated() {
        assertEquals(401, controller(mock(MultiTenantAuthService.class)).validatePlatformSession(new MockHttpServletRequest()).getCode());
    }

    private BladeResult<Map<String,Object>> validate(RuntimeException error) {
        MultiTenantAuthService service = mock(MultiTenantAuthService.class);
        when(service.validatePlatformSession("fixture-local")).thenThrow(error);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization","Bearer fixture-local");
        request.addHeader("accesstoken","fixture-platform");
        BladeResult<Map<String,Object>> result = controller(service).validatePlatformSession(request);
        verify(service).validatePlatformSession("fixture-local");
        return result;
    }

    private SsoCompatController controller(MultiTenantAuthService service) {
        TokenProperties properties = new TokenProperties(); properties.setTenantType("multi");
        return new SsoCompatController(mock(BladeTokenUserStore.class),properties,service);
    }

    private void assertTrace(BladeResult<Map<String,Object>> result) {
        assertNotNull(UUID.fromString((String) result.getData().get("traceId")));
    }
}
