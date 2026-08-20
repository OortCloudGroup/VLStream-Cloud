package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.vlstream.test.vlstream.config.VlsEventReportProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class ThirdPartyEventReportClientTest {

	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void reportsAnonymouslyWithStableIdempotencyKey() throws Exception {
		AtomicReference<String> accessToken = new AtomicReference<>();
		AtomicReference<String> idempotencyKey = new AtomicReference<>();
		AtomicReference<String> requestBody = new AtomicReference<>();
		startServer(exchange -> {
			accessToken.set(exchange.getRequestHeaders().getFirst("AccessToken"));
			idempotencyKey.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
			requestBody.set(readBody(exchange.getRequestBody()));
			respond(exchange, 200, "{\"code\":200}");
		});

		ThirdPartyEventReportClient client = new ThirdPartyEventReportClient(properties());
		JSONObject event = new JSONObject().set("device_id", "CAM-1");
		client.report(event, "vls-event:tenant-a:1");

		assertEquals(null, accessToken.get());
		assertEquals("vls-event:tenant-a:1", idempotencyKey.get());
		JSONObject body = JSONUtil.parseObj(requestBody.get());
		assertEquals("CAM-1", body.getJSONArray("event_report").getJSONObject(0).getStr("device_id"));
	}

	@Test
	void rejectsBusinessFailureEvenWhenHttpStatusIsOk() throws Exception {
		startServer(exchange -> respond(exchange, 200, "{\"code\":500,\"msg\":\"rejected\"}"));
		ThirdPartyEventReportClient client = new ThirdPartyEventReportClient(properties());

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> client.report(new JSONObject(), "event-1"));

		assertEquals("rejected", exception.getMessage());
	}

	private VlsEventReportProperties properties() {
		VlsEventReportProperties properties = new VlsEventReportProperties();
		properties.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/report");
		properties.setTimeoutMillis(2000);
		return properties;
	}

	private void startServer(ExchangeHandler handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/report", exchange -> handler.handle(exchange));
		server.start();
	}

	private String readBody(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = input.read(buffer)) != -1) {
			output.write(buffer, 0, read);
		}
		return new String(output.toByteArray(), StandardCharsets.UTF_8);
	}

	private void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(bytes);
		}
	}

	private interface ExchangeHandler {
		void handle(HttpExchange exchange) throws IOException;
	}
}
