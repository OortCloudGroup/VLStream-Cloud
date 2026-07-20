/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsWebRtcProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VlsWebRtcGatewayServiceTest {

	private HttpServer server;

	/** Stops the local probe server after each test. */
	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	/** Verifies that a real successful HTTP response is reported as available. */
	@Test
	void reportsAvailableGatewayAndPublicUrl() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/getMediaList", exchange -> {
			byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();

		VlsWebRtcProperties properties = properties("http://127.0.0.1:" + server.getAddress().getPort());
		VlsWebRtcGatewayService service = new VlsWebRtcGatewayService(properties);
		Map<String, Object> config = service.getPublicConfig();

		assertTrue((Boolean) config.get("available"));
		assertEquals("/bus/webrtc-streamer-server", config.get("serverUrl"));
		assertEquals("webrtc-streamer", config.get("provider"));
	}

	/** Verifies that an unreachable gateway is not masked as a successful service. */
	@Test
	void reportsUnavailableGatewayWithoutFakingSuccess() {
		VlsWebRtcProperties properties = properties("http://127.0.0.1:1");
		properties.setConnectTimeoutMillis(100);
		properties.setReadTimeoutMillis(100);

		Map<String, Object> status = new VlsWebRtcGatewayService(properties).getStatus();

		assertFalse((Boolean) status.get("available"));
		assertTrue(String.valueOf(status.get("message")).contains("不可用"));
	}

	/** Creates isolated settings for a local health probe. */
	private VlsWebRtcProperties properties(String internalUrl) {
		VlsWebRtcProperties properties = new VlsWebRtcProperties();
		properties.setInternalBaseUrl(internalUrl);
		properties.setPublicBaseUrl("/bus/webrtc-streamer-server/");
		properties.setHealthCacheMillis(0L);
		return properties;
	}
}
