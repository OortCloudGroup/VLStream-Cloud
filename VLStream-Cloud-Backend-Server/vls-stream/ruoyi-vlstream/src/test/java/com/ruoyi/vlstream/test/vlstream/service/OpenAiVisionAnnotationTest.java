/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class OpenAiVisionAnnotationTest {
    private final LlmApiKeyCipher cipher = mock(LlmApiKeyCipher.class);
    private final OpenAiVisionClient client = new OpenAiVisionClient(cipher);
    private final LlmProvider provider = new LlmProvider();
    private HttpServer server;
    private byte[] png;

    @BeforeEach
    void startFixture() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        provider.setApiKeyCiphertext("ciphertext");
        provider.setEnabled(true);
        provider.setModelName("review-model");
        when(cipher.decrypt("ciphertext")).thenReturn("fixture-key");
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_BYTE_INDEXED), "png", image);
        png = image.toByteArray();
    }

    @AfterEach
    void stopFixture() {
        server.stop(0);
    }

    @Test
    void streamsTextPageCodeThroughTheSelectedRelayModel() {
        AtomicReference<JSONObject> sent = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            sent.set(JSONUtil.parseObj(new String(org.apache.commons.io.IOUtils.toByteArray(exchange.getRequestBody()), StandardCharsets.UTF_8)));
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            reply(exchange, 200, "data: {\"choices\":[{\"delta\":{\"content\":\"第一行\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"第二行\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n");
        });
        java.util.List<String> chunks = new java.util.ArrayList<>();
        client.generateScene(provider, "scene-model", "instructions", "description", chunks::add);
        assertEquals(java.util.Arrays.asList("第一行", "第二行"), chunks);
        assertEquals("scene-model", sent.get().getStr("model"));
        assertTrue(sent.get().getBool("stream"));
        assertEquals(Integer.valueOf(16384), sent.get().getInt("max_tokens"));
        assertFalse(sent.get().containsKey("thinking"));
        assertFalse(sent.get().containsKey("reasoning_effort"));
        assertEquals("system", sent.get().getJSONArray("messages").getJSONObject(0).getStr("role"));
    }

    @Test
    void reservesOutputForCodeUsingTheSelectedGlmModelsSupportedThinkingControls() {
        AtomicReference<JSONObject> sent = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            sent.set(JSONUtil.parseObj(new String(org.apache.commons.io.IOUtils.toByteArray(exchange.getRequestBody()), StandardCharsets.UTF_8)));
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            reply(exchange, 200, "data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"internal\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"page\"},\"finish_reason\":\"stop\"}]}\n\n");
        });
        StringBuilder text = new StringBuilder();
        client.generateScene(provider, "glm-5.3-Flash", "instructions", "description", text::append);
        assertEquals("page", text.toString());
        assertEquals(Integer.valueOf(32768), sent.get().getInt("max_tokens"));
        assertEquals("low", sent.get().getStr("reasoning_effort"));
        assertFalse(sent.get().containsKey("thinking"));

        client.generateScene(provider, "GLM-4.7", "instructions", "description", text::append);
        assertEquals("disabled", sent.get().getByPath("thinking.type"));
        assertFalse(sent.get().containsKey("reasoning_effort"));
        assertEquals("review-model", provider.getModelName());
    }

    @Test
    void readsTheFinalPageAfterLargeStreamedReasoningWithoutShowingReasoningInThePage() {
        server.createContext("/v1/chat/completions", exchange -> {
            String padding = org.apache.commons.lang3.StringUtils.repeat("r", 4096);
            byte[] reasoning = ("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"" + padding + "\"}}]}\n\n")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream stream = exchange.getResponseBody()) {
                for (int i = 0; i < 512; i++) stream.write(reasoning);
                stream.write("data: {\"choices\":[{\"delta\":{\"content\":\"page\"},\"finish_reason\":\"stop\"}]}\n\n"
                    .getBytes(StandardCharsets.UTF_8));
            }
        });
        StringBuilder visible = new StringBuilder();
        client.generateScene(provider, "glm-5.3-flash", "instructions", "description", visible::append);
        assertEquals("page", visible.toString());
    }

    @Test
    void distinguishesOutputExhaustionFilteringAndUpstreamNetworkFailuresAcrossRelayFormats() {
        AtomicReference<String> reason = new AtomicReference<>();
        AtomicReference<Boolean> streaming = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            String entry = "{\"choices\":[{\"finish_reason\":\"" + reason.get() + "\",\"message\":{\"content\":\"incomplete\"}}]}";
            exchange.getResponseHeaders().set("Content-Type", streaming.get() ? "text/event-stream" : "application/json");
            reply(exchange, 200, streaming.get() ? "data: " + entry + "\n\ndata: [DONE]\n\n" : entry);
        });
        for (boolean sse : new boolean[] {true, false}) {
            streaming.set(sse);
            for (String[] scenario : new String[][] {{"length", "输出长度"}, {"content_filter", "过滤"},
                {"sensitive", "过滤"}, {"network_error", "连接中断"}, {"model_context_window_exceeded", "上下文"}}) {
                reason.set(scenario[0]);
                StringBuilder text = new StringBuilder();
                ServiceException failure = assertThrows(ServiceException.class,
                    () -> client.generateScene(provider, "scene-model", "instructions", "description", text::append));
                assertTrue(failure.getMessage().contains(scenario[1]), failure.getMessage());
                assertEquals(0, text.length());
            }
        }
    }

    @Test
    void sendsIntactPngWithSelectedModelAndReturnsRawText() {
        AtomicReference<JSONObject> sent = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try (InputStream input = exchange.getRequestBody()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
                sent.set(JSONUtil.parseObj(new String(bytes.toByteArray(), StandardCharsets.UTF_8)));
            }
            reply(exchange, 200, "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"label\\\":\\\"1\\\"}\"}}]}");
        });

        assertEquals("{\"label\":\"1\"}", client.annotate(provider, "annotation-model", "classify", png));
        assertEquals("Bearer fixture-key", authorization.get());
        assertEquals("annotation-model", sent.get().getStr("model"));
        assertEquals("review-model", provider.getModelName());
        assertEquals("classify", sent.get().getByPath("messages[0].content[0].text"));
        assertEquals("high", sent.get().getByPath("messages[0].content[1].image_url.detail"));
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(png),
            sent.get().getByPath("messages[0].content[1].image_url.url"));
    }

    @Test
    void propagatesQuotaStatusWithoutExposingUpstreamErrorOrRetrying() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            reply(exchange, 429, "provider error including fixture-key");
        });
        ServiceException exception = assertThrows(ServiceException.class,
            () -> client.annotate(provider, "annotation-model", "classify", png));
        assertEquals(Integer.valueOf(429), exception.getCode());
        assertFalse(exception.getMessage().contains("fixture-key"));
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotSendImageOrKeyToRedirectTarget() {
        AtomicInteger redirected = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("Location", "/redirect");
            reply(exchange, 307, "");
        });
        server.createContext("/redirect", exchange -> {
            redirected.incrementAndGet();
            reply(exchange, 200, "{}");
        });
        assertEquals(Integer.valueOf(307), assertThrows(ServiceException.class,
            () -> client.annotate(provider, "annotation-model", "classify", png)).getCode());
        assertEquals(0, redirected.get());
    }

    @Test
    void rejectsTruncatedEnvelopeAndOversizedImage() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            reply(exchange, 200, "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}");
        });
        assertTrue(assertThrows(ServiceException.class, () -> client.annotate(
            provider, "annotation-model", "classify", png)).getMessage().contains("截断"));
        assertThrows(ServiceException.class, () -> client.annotate(
            provider, "annotation-model", "classify", new byte[OpenAiVisionClient.MAX_ANNOTATION_IMAGE_BYTES + 1]));
        assertEquals(1, calls.get());
    }

    @Test
    void boundsProviderEnvelopeNestingBeforeReadingContent() {
        StringBuilder response = new StringBuilder("{\"nested\":");
        for (int index = 0; index < 100; index++) response.append('[');
        response.append("null");
        for (int index = 0; index < 100; index++) response.append(']');
        response.append('}');
        server.createContext("/v1/chat/completions", exchange -> reply(exchange, 200, response.toString()));
        assertTrue(assertThrows(ServiceException.class, () -> client.annotate(
            provider, "annotation-model", "classify", png)).getMessage().contains("嵌套层级"));
    }

    private void reply(HttpExchange exchange, int status, String response) throws IOException {
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
