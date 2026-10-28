package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.framework.config.properties.TokenProperties;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class PlatformTenantFailureClassificationTest {
    private HttpServer server;
    private PlatformTenantClient client;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("{\"code\":200,\"data\":{\"userId\":\"user\",\"tenantId\":\"tenant\"}}");
    private final PlatformGatewayHeaders headers = new PlatformGatewayHeaders("app", "fixture-app", "fixture-secret");
    private final AtomicReference<JSONObject> receivedBody = new AtomicReference<>();
    private final AtomicReference<Headers> receivedHeaders = new AtomicReference<>();
    private final AtomicReference<String> receivedMethod = new AtomicReference<>();

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/verify", exchange -> {
            receivedBody.set(JSONUtil.parseObj(IoUtil.read(exchange.getRequestBody(), StandardCharsets.UTF_8)));
            receivedHeaders.set(exchange.getRequestHeaders());
            receivedMethod.set(exchange.getRequestMethod());
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        TokenProperties properties = new TokenProperties();
        properties.setMultiTenantVerifyTokenAddress("http://127.0.0.1:" + server.getAddress().getPort() + "/verify");
        properties.setMultiTenantConnectTimeoutMillis(300); properties.setMultiTenantReadTimeoutMillis(300);
        client = new PlatformTenantClient(properties);
    }

    @AfterEach void cleanup() { if (server != null) server.stop(0); }

    @Test void verificationSendsTheBoundPlatformCredentialInTheLegacySsoBodyAndGatewayHeaders() {
        for (String token : new String[] {"fixture-platform-before-switch", "fixture-platform-after-switch"}) {
            assertEquals("tenant", client.verifyToken(token, headers).getTenantId());
            assertEquals("POST", receivedMethod.get());
            assertEquals(token, receivedBody.get().getStr("accessToken"));
            assertEquals(token, receivedHeaders.get().getFirst("accesstoken"));
            assertEquals("fixture-app", receivedHeaders.get().getFirst("appid"));
            assertEquals("fixture-secret", receivedHeaders.get().getFirst("secretkey"));
            assertEquals("app", receivedHeaders.get().getFirst("requesttype"));
        }
    }

    @Test void successStillRequiresVerifiedUserAndTenant() {
        assertEquals("tenant", client.verifyToken("fixture-token", headers).getTenantId());
        body.set("{\"code\":200,\"data\":{\"userId\":\"user\"}}");
        PlatformTenantCallException error = failure();
        assertEquals(PlatformTenantCallException.Kind.RESPONSE_FORMAT, error.getKind());
        assertEquals(502, error.responseCode());
    }

    @Test void onlyExplicitAuthenticationBusinessCodesRequestReauthentication() {
        for (int code : new int[] {401, 4004, 4444, 403, 500, 7777}) {
            body.set("{\"code\":" + code + ",\"msg\":\"fixture-secret fixture-token\"}");
            PlatformTenantCallException error = failure();
            assertEquals(Integer.valueOf(code), error.getBusinessCode());
            assertEquals(code == 401 || code == 4004 || code == 4444 || code == 403 ? code : 502, error.responseCode());
            assertFalse(error.getMessage().contains("fixture-secret"));
            assertFalse(error.getMessage().contains("fixture-token"));
        }
    }

    @Test void upstreamHttpPermissionAndOutagesAreNotRelabeledAsExpiredCredentials() {
        for (int code : new int[] {401, 403, 404, 503}) {
            status.set(code);
            PlatformTenantCallException error = failure();
            assertEquals(Integer.valueOf(code), error.getHttpStatus());
            assertEquals(code == 401 || code == 403 ? code : 502, error.responseCode());
        }
    }

    @Test void malformedOrMissingBusinessReceiptIsAProtocolFailure() {
        for (String invalid : new String[] {"<html>gateway</html>", "", "{}", "{\"code\":\"invalid\"}"}) {
            body.set(invalid);
            PlatformTenantCallException error = failure();
            assertEquals(PlatformTenantCallException.Kind.RESPONSE_FORMAT, error.getKind());
            assertEquals(502, error.responseCode());
        }
    }

    @Test void missingConfigurationAndTransportFailureRemainServiceFailures() {
        PlatformTenantClient unconfigured = new PlatformTenantClient(new TokenProperties());
        assertEquals(503, assertThrows(PlatformTenantCallException.class,
            () -> unconfigured.verifyToken("fixture-token", headers)).responseCode());
        server.stop(0); server = null;
        PlatformTenantCallException error = failure();
        assertEquals(PlatformTenantCallException.Kind.TRANSPORT, error.getKind());
        assertEquals(503, error.responseCode());
    }

    private PlatformTenantCallException failure() {
        return assertThrows(PlatformTenantCallException.class, () -> client.verifyToken("fixture-token", headers));
    }
}
