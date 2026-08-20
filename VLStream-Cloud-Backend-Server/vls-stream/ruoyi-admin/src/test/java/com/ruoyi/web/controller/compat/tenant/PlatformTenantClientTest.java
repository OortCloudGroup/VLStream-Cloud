/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.framework.config.properties.TokenProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class PlatformTenantClientTest {

    private HttpServer server;
    private PlatformTenantClient client;
    private final AtomicReference<String> appId = new AtomicReference<String>();
    private final AtomicReference<String> secretKey = new AtomicReference<String>();
    private final AtomicReference<String> requestType = new AtomicReference<String>();
    private final AtomicReference<String> accessToken = new AtomicReference<String>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/verify", jsonHandler(
            "{\"code\":200,\"data\":{\"userId\":\"user-1\",\"tenantId\":\"tenant-a\",\"userName\":\"platform-admin\"}}"));
        server.createContext("/tenants", jsonHandler(
            "{\"code\":200,\"data\":{\"list\":[{\"tenant_id\":\"tenant-a\",\"tenant_name\":\"Tenant A\",\"user_id\":\"user-1\",\"status\":1}]}}"));
        server.createContext("/login", jsonHandler(
            "{\"code\":200,\"data\":{\"accessToken\":\"tenant-token\"}}"));
        server.createContext("/user", jsonHandler(
            "{\"code\":200,\"data\":{\"userInfo\":{\"realName\":\"Platform Admin\"}}}"));
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        TokenProperties properties = new TokenProperties();
        properties.setMultiTenantVerifyTokenAddress(baseUrl + "/verify");
        properties.setMultiTenantUserTenantsUrl(baseUrl + "/tenants");
        properties.setMultiTenantLoginUrl(baseUrl + "/login");
        properties.setMultiTenantAdminUserUrl(baseUrl + "/user");
        client = new PlatformTenantClient(properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void forwardsGatewayHeadersAndParsesPlatformContracts() {
        PlatformGatewayHeaders headers = new PlatformGatewayHeaders("app", "gateway-app", "gateway-secret");

        PlatformIdentity identity = client.verifyToken("platform-token", headers);
        List<PlatformTenant> tenants = client.getUserTenants("platform-token", headers);
        String selectedToken = client.selectTenant("platform-token", "tenant-a", headers);
        String displayName = client.loadDisplayName("tenant-token", "user-1", headers);

        assertEquals("user-1", identity.getUserId());
        assertEquals("tenant-a", identity.getTenantId());
        assertEquals(1, tenants.size());
        assertEquals("Tenant A", tenants.get(0).getTenantName());
        assertEquals("tenant-token", selectedToken);
        assertEquals("Platform Admin", displayName);
        assertEquals("gateway-app", appId.get());
        assertEquals("gateway-secret", secretKey.get());
        assertEquals("app", requestType.get());
        assertEquals("tenant-token", accessToken.get());
    }

    private HttpHandler jsonHandler(final String response) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                appId.set(exchange.getRequestHeaders().getFirst("appid"));
                secretKey.set(exchange.getRequestHeaders().getFirst("secretkey"));
                requestType.set(exchange.getRequestHeaders().getFirst("requestType"));
                accessToken.set(exchange.getRequestHeaders().getFirst("AccessToken"));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            }
        };
    }
}
