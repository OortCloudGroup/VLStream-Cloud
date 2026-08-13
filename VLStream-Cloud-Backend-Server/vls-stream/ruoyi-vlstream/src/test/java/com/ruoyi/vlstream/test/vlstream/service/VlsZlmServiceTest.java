package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsZlmProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class VlsZlmServiceTest {

	@Test
	void createsAutoClosingProxyAndBrowserUrl() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/index/api/getMediaInfo", exchange -> {
			byte[] body = "{\"code\":-1,\"msg\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/index/api/addStreamProxy", exchange -> {
			String query = exchange.getRequestURI().getRawQuery();
			assertTrue(query.contains("auto_close=1"));
			byte[] body = "{\"code\":0,\"data\":{\"key\":\"__defaultVhost__/vlstream/mqtt_7\"}}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		try {
			VlsZlmProperties properties = new VlsZlmProperties();
			properties.setSecret("test-secret");
			properties.setInternalBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
			properties.setPublicBaseUrl("https://media.example.test");
			Map<String, Object> result = new VlsZlmService(properties).createProxy(7L, "rtsp://camera/live");
			assertEquals("__defaultVhost__/vlstream/mqtt_7", result.get("proxyKey"));
			assertEquals("https://media.example.test/index/api/webrtc?app=vlstream&stream=mqtt_7&type=play", result.get("webrtcUrl"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void createsNamespacedProxyForCustomDevice() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/index/api/getMediaInfo", exchange -> {
			byte[] body = "{\"code\":-1}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/index/api/addStreamProxy", exchange -> {
			assertTrue(exchange.getRequestURI().getRawQuery().contains("stream=custom_device_99"));
			byte[] body = "{\"code\":0,\"data\":{\"key\":\"proxy-key\"}}"
				.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		try {
			VlsZlmProperties properties = new VlsZlmProperties();
			properties.setSecret("test-secret");
			properties.setInternalBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
			properties.setPublicBaseUrl("https://media.example.test");
			Map<String, Object> result = new VlsZlmService(properties)
				.createProxy("custom_device_99", "rtsp://camera/live");
			assertEquals("https://media.example.test/index/api/webrtc?app=vlstream&stream=custom_device_99&type=play",
				result.get("webrtcUrl"));
		} finally {
			server.stop(0);
		}
	}
}
