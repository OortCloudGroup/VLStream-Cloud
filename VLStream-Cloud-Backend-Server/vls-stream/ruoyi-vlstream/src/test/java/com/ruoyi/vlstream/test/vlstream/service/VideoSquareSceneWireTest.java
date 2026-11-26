package com.ruoyi.vlstream.test.vlstream.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.spring.SaTokenContextForSpring;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.controller.VideoSquareSceneController;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.Filter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
@Isolated // The servlet fixture installs a temporary Sa-Token context.
class VideoSquareSceneWireTest {
    private static final String PATH = "/vls/video-square/scenes/topology";
    private static final String CAMERA = "ONVIF:7";
    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final CountDownLatch previewFlushed = new CountDownLatch(1);

    @Test
    void streamsPersistsAndRevisesAnAuthorizedCameraThroughTheHttpContract() throws Exception {
        HttpServer model = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        model.createContext("/v1/chat/completions", this::reply);
        model.start();
        EmbeddedDatabase database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
            .setType(EmbeddedDatabaseType.H2).build();
        SaTokenContext original = SaManager.getSaTokenContext();
        String previousTenant = TenantContextHolder.getTenantId();
        try {
            SaManager.setSaTokenContext(new SaTokenContextForSpring());
            TenantContextHolder.setTenantId("000000");
            JdbcTemplate jdbc = new JdbcTemplate(database);
            jdbc.execute("CREATE TABLE vls_video_square_scene (tenant_id varchar(64), owner_id varchar(64), "
                + "view_mode varchar(16), revision int, prompt text, scene_json text, PRIMARY KEY(tenant_id,owner_id,view_mode))");
            AuthorizedVideoCatalog catalog = mock(AuthorizedVideoCatalog.class);
            when(catalog.page(1)).thenReturn(json.readTree("{\"total\":1,\"rows\":[{\"id\":\"ONVIF:7\",\"deviceName\":\"东门\"}]}"));
            VlsLlmReviewProperties settings = new VlsLlmReviewProperties();
            settings.setEncryptionKey(UUID.randomUUID().toString().replace("-", ""));
            LlmApiKeyCipher cipher = new LlmApiKeyCipher(settings);
            LlmProvider provider = new LlmProvider();
            provider.setBaseUrl("http://127.0.0.1:" + model.getAddress().getPort() + "/v1");
            provider.setModelName("fixture-model");
            provider.setEnabled(true);
            provider.setApiKeyCiphertext(cipher.encrypt("fixture"));
            LlmSystemProviderService providers = mock(LlmSystemProviderService.class);
            when(providers.requireAuthorized()).thenReturn(provider);
            VideoSquareSceneService service = new VideoSquareSceneService(jdbc, catalog, providers, new OpenAiVisionClient(cipher));
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new VideoSquareSceneController(service, json))
                .defaultRequest(get(PATH).requestAttr(LoginHelper.USER_KEY, "101"))
                .addFilters(observePreview()).build();

            JsonNode first = generate(mvc, 0, "把东门放入园区拓扑");
            assertTrue(first.at("/scene/html").asText().contains("<main id='campus'>"));
            assertEquals(first, latest(mvc));
            JsonNode revised = generate(mvc, 1, "保留摄像头，把预览移到左侧");
            assertTrue(revised.at("/scene/html").asText().contains("<aside id='inspector' data-vls-preview>"));
            assertEquals(revised, latest(mvc));
            assertEquals(2, requests.size());
            assertEquals("system", requests.get(1).at("/messages/0/role").asText());
            String input = requests.get(1).at("/messages/1/content").asText();
            assertTrue(input.contains("<current_page>\n" + first.at("/scene/html").asText() + "\n</current_page>"));
            assertTrue(input.contains("1. " + first.path("prompt").asText()));
            assertTrue(input.contains("## 本次要求\n" + revised.path("prompt").asText()));
            assertTrue(input.contains("- c1：东门"));
            assertFalse(input.contains(CAMERA));
            assertEquals(1, revised.at("/scene/undoCount").asInt());
            assertFalse(revised.path("scene").has("undo"));
            JsonNode restored = undo(mvc, 2);
            assertEquals(first.at("/scene/html"), restored.at("/scene/html"));
            assertEquals(restored, latest(mvc));
        } finally {
            SaManager.setSaTokenContext(original);
            TenantContextHolder.setTenantId(previousTenant);
            model.stop(0);
            database.shutdown();
        }
    }

    private Filter observePreview() {
        return (request, response, chain) -> {
            MockHttpServletResponse captured = (MockHttpServletResponse) response;
            ServletOutputStream stream = new DelegatingServletOutputStream(captured.getOutputStream()) {
                @Override
                public void flush() throws IOException {
                    super.flush();
                    if (captured.getContentAsString().contains("\"type\":\"preview\"")) previewFlushed.countDown();
                }
            };
            chain.doFilter(request, new HttpServletResponseWrapper(captured) {
                @Override
                public ServletOutputStream getOutputStream() { return stream; }
            });
        };
    }

    private JsonNode generate(MockMvc mvc, int revision, String prompt) throws Exception {
        ObjectNode request = json.createObjectNode().put("revision", revision).put("prompt", prompt);
        request.putArray("deviceIds").add(CAMERA);
        byte[] response = mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/x-ndjson"))
            .andReturn().getResponse().getContentAsByteArray();
        List<JsonNode> events = new ArrayList<>();
        for (String line : new String(response, StandardCharsets.UTF_8).trim().split("\n")) events.add(json.readTree(line));
        assertTrue(events.size() >= 3, events.toString());
        assertEquals("started", events.get(0).path("type").asText());
        assertEquals("preview", events.get(1).path("type").asText());
        assertEquals("web-page-v1", events.get(1).at("/scene/format").asText());
        assertFalse(events.get(1).path("scene").has("cameras"));
        JsonNode last = events.get(events.size() - 1);
        assertEquals("saved", last.path("type").asText(), last.toString());
        JsonNode saved = last.path("value");
        assertEquals(revision + 1, saved.path("revision").asInt());
        assertEquals(prompt, saved.path("prompt").asText());
        assertEquals(CAMERA, saved.at("/scene/cameras/c1").asText());
        assertEquals("fixture-model", saved.at("/scene/generatedWith").asText());
        return saved;
    }

    private JsonNode undo(MockMvc mvc, int revision) throws Exception {
        byte[] body = mvc.perform(post(PATH + "/undo").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(json.createObjectNode().put("revision", revision))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        JsonNode data = json.readTree(body).path("data");
        assertEquals(revision + 1, data.path("revision").asInt(), data.toString());
        return data;
    }

    private JsonNode latest(MockMvc mvc) throws Exception {
        byte[] body = mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return json.readTree(body).path("data");
    }

    private void reply(HttpExchange exchange) throws IOException {
        try {
            JsonNode request = json.readTree(exchange.getRequestBody());
            if (!request.path("stream").asBoolean() || !"fixture-model".equals(request.path("model").asText())
                || !"Bearer fixture".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            requests.add(request);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                boolean first = requests.size() == 1;
                writeDelta(out, "<!doctype html><html><head><title>园区</title></head><body>"
                    + (first ? "<main id='campus'>" : "<aside id='inspector' data-vls-preview>"));
                // Withhold the rest of the upstream result until the client has received a draft.
                if (first && !previewFlushed.await(5, TimeUnit.SECONDS)) return;
                writeDelta(out, "<button id='cam'>东门</button>" + (first ? "</main>" : "</aside>")
                    + "<script>document.getElementById('cam').addEventListener('click',()=>VLS.preview('c1'))</script></body></html>");
                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException(interrupted);
        } finally {
            exchange.close();
        }
    }

    private void writeDelta(OutputStream out, String record) throws IOException {
        ObjectNode chunk = json.createObjectNode();
        chunk.putArray("choices").addObject().putObject("delta").put("content", record + "\n");
        out.write(("data: " + chunk + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
