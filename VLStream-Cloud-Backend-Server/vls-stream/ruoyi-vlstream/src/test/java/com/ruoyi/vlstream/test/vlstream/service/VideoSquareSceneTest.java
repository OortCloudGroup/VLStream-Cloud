package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class VideoSquareSceneTest {
    private static final String START = "<!doctype html><html><head><title>园区 &amp; 入口</title></head><body>";
    private static final String END = "</body></html>";
    private static final String HTML = START + "<button id='cam'>东门</button>"
        + "<script>document.getElementById('cam').addEventListener('click', () => window.VLS.preview('c1'));</script>" + END;
    private final AuthorizedVideoCatalog catalog = mock(AuthorizedVideoCatalog.class);
    private final LlmSystemProviderService providers = mock(LlmSystemProviderService.class);
    private final OpenAiVisionClient model = mock(OpenAiVisionClient.class);
    private final LlmProvider provider = new LlmProvider();
    private final VideoSquareSceneService.GenerateRequest request = new VideoSquareSceneService.GenerateRequest();
    private final List<String> inputs = new ArrayList<>();
    private final Deque<String> outputs = new ArrayDeque<>();
    private String output = HTML;
    private JdbcTemplate jdbc;
    private VideoSquareSceneService service;

    @BeforeEach
    void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE vls_video_square_scene (tenant_id varchar(64),owner_id varchar(64),view_mode varchar(16),"
            + "revision int,prompt text,scene_json text,PRIMARY KEY(tenant_id,owner_id,view_mode))");
        service = new VideoSquareSceneService(jdbc, catalog, providers, model);
        request.setRevision(0);
        request.setPrompt("生成交互园区");
        provider.setModelName("selected-tenant-model");
        when(providers.requireAuthorized()).thenReturn(provider);
        doAnswer(call -> {
            inputs.add(call.<String>getArgument(3));
            String reply = outputs.isEmpty() ? output : outputs.poll();
            // Deliver in uneven pieces, as an upstream stream would.
            for (int i = 0; i < reply.length(); i += 37) call.<Consumer<String>>getArgument(4).accept(reply.substring(i, Math.min(reply.length(), i + 37)));
            return null;
        }).when(model).generateScene(eq(provider), anyString(), anyString(), anyString(), any());
    }

    private VideoSquareSceneService.SavedScene generate(String mode) {
        return service.generate("A", "1", mode, request, draft -> {});
    }

    private JsonNode catalogRows(String... ids) {
        ObjectNode data = VideoSquareSceneFormat.JSON.createObjectNode().put("total", ids.length);
        for (String id : ids) data.withArray("rows").addObject().put("id", id).put("deviceName", "东门镜头")
            .put("url", "private-stream");
        data.withArray("rows");
        return data;
    }

    @Test
    void resolvesOnlyAuthorizedBindingsAndKeepsPersonalViewsIndependent() {
        request.setDeviceIds(Arrays.asList("ONVIF:7", "OTHER:9"));
        when(catalog.page(1)).thenReturn(catalogRows("ONVIF:7"));
        JsonNode scene = generate("topology").getScene();
        assertEquals("ONVIF:7", scene.at("/cameras/c1").asText());
        assertEquals(1, scene.get("cameras").size());
        assertEquals(HTML, scene.get("html").asText());
        assertEquals("园区 & 入口", scene.get("title").asText());
        assertTrue(inputs.get(0).contains("- c1：东门镜头"));
        assertFalse(inputs.get(0).contains("<current_page>"));
        for (String hidden : Arrays.asList("OTHER:9", "ONVIF:7", "private-stream")) assertFalse(inputs.get(0).contains(hidden));
        assertEquals(1, service.latest("A", "1", "topology").getRevision());
        assertNull(service.latest("B", "1", "topology").getScene());
        assertNull(service.latest("A", "2", "topology").getScene());
        assertNull(service.latest("A", "1", "bim").getScene());
        request.setRevision(1);
        generate("topology");
        assertTrue(inputs.get(1).contains("<current_page>\n" + HTML + "\n</current_page>"));
        assertFalse(inputs.get(1).contains("ONVIF:7"));
        assertTrue(inputs.get(1).contains("## 历次要求（由旧到新，均已体现在当前页面中，需继续保留）\n1. 生成交互园区"));
        assertFalse(service.latest("A", "1", "topology").getScene().has("undo"));
        assertEquals(1, service.latest("A", "1", "topology").getScene().get("undoCount").asInt());
    }

    @Test
    void substitutesAnAliasWhenTheDirectoryUsesTheRealIdAsTheDisplayName() {
        request.setDeviceIds(Collections.singletonList("ONVIF:7"));
        JsonNode rows = catalogRows("ONVIF:7");
        ((ObjectNode) rows.at("/rows/0")).put("deviceName", "ONVIF:7");
        when(catalog.page(1)).thenReturn(rows);
        assertEquals("ONVIF:7", generate("bim").getScene().at("/cameras/c1").asText());
        assertTrue(inputs.get(0).contains("- c1：c1"));
        assertFalse(inputs.get(0).contains("ONVIF:7"));
    }

    @Test
    void failureAndStaleWritesPreserveTheLastGoodScene() {
        generate("bim");
        assertEquals(Integer.valueOf(409), assertThrows(ServiceException.class, () -> generate("bim")).getCode());
        request.setRevision(1);
        output = START + "<canvas></canvas>";
        assertThrows(ServiceException.class, () -> generate("bim"));
        when(providers.requireAuthorized()).thenThrow(new ServiceException("OortCloud 已取消授权"));
        assertThrows(ServiceException.class, () -> generate("bim"));
        assertEquals(1, service.latest("A", "1", "bim").getRevision());
        assertEquals(2, inputs.size());
    }

    @Test
    void retainsAuthorizedAliasesOnReorderAndNeverReassignsARevokedAlias() {
        when(catalog.page(1)).thenReturn(catalogRows("CAM:1", "CAM:2"), catalogRows("CAM:3", "CAM:2"))
            .thenThrow(new ServiceException("设备目录不可用", 503)).thenReturn(catalogRows("CAM:4", "CAM:2"));
        request.setDeviceIds(Arrays.asList("CAM:1", "CAM:2"));
        generate("bim");
        request.setRevision(1);
        request.setDeviceIds(Collections.singletonList("CAM:3"));
        JsonNode bindings = generate("bim").getScene().get("cameras");
        assertFalse(bindings.has("c1"));
        assertEquals("CAM:2", bindings.path("c2").asText());
        assertEquals("CAM:3", bindings.path("c3").asText());
        assertFalse(inputs.get(1).contains("CAM:"));
        request.setRevision(2);
        request.setDeviceIds(Collections.singletonList("CAM:4"));
        assertEquals(Integer.valueOf(503), assertThrows(ServiceException.class, () -> generate("bim")).getCode());
        assertEquals(2, service.latest("A", "1", "bim").getRevision());
        assertEquals("CAM:4", generate("bim").getScene().at("/cameras/c4").asText());
    }

    @Test
    void convertsLegacySceneBySendingItsStructureWithOnlyCurrentAliases() {
        jdbc.update("INSERT INTO vls_video_square_scene VALUES(?,?,?,?,?,?)", "A", "1", "topology", 3, "原场景",
            "{\"title\":\"旧版\",\"nodes\":[{\"deviceId\":\"ONVIF:7\"},{\"deviceId\":\"REVOKED:1\"}]}");
        assertEquals("旧版", service.latest("A", "1", "topology").getScene().get("title").asText());
        request.setRevision(3);
        when(catalog.page(1)).thenReturn(catalogRows("ONVIF:7"));
        JsonNode scene = generate("topology").getScene();
        assertEquals("web-page-v1", scene.get("format").asText());
        String legacy = inputs.get(0).substring(inputs.get(0).indexOf("## 旧版结构化场景"));
        assertTrue(legacy.contains("{\"camera\":\"c1\"}"));
        assertTrue(legacy.contains("{\"camera\":null}"));
        assertTrue(inputs.get(0).contains("1. 原场景"));
        assertFalse(inputs.get(0).contains("ONVIF:7"));
        assertFalse(inputs.get(0).contains("REVOKED:1"));
    }

    @Test
    void streamsBoundedInertSnapshotsAndAssemblesRawOrFencedHtmlAcrossAnyChunkBoundary() {
        for (String wire : Arrays.asList(HTML, "```html\n" + HTML + "\n```")) {
            List<JsonNode> drafts = new ArrayList<>();
            VideoSquareSceneStream stream = new VideoSquareSceneStream("bim", drafts::add);
            for (char c : wire.toCharArray()) stream.accept(String.valueOf(c));
            assertTrue(drafts.size() > 0 && drafts.size() < wire.length() / 10);
            assertFalse(drafts.get(0).has("cameras"));
            assertEquals(HTML, stream.finish().get("html").asText());
        }
    }

    @Test
    void malformedOutputCannotBecomeSavedHtml() {
        for (String invalid : Arrays.asList(START, START + END, "<html><body>no title</body></html>", "{}",
            "这是一段很长的解释。".concat(String.join("", Collections.nCopies(400, "说明"))) + HTML)) {
            VideoSquareSceneStream stream = new VideoSquareSceneStream("topology", s -> {});
            assertThrows(ServiceException.class, () -> { stream.accept(invalid); stream.finish(); });
        }
        assertThrows(ServiceException.class, () -> service.latest("A", "1", "map"));
    }

    @Test
    void selectsAnAuthorizedModelForOnlyThisViewAndLeavesTheTenantDefaultAlone() {
        when(model.listSceneModels(provider)).thenReturn(Collections.singletonList("scene-chat"));
        assertEquals("selected-tenant-model", service.models().getDefaultModel());
        request.setModel("unlisted");
        assertThrows(ServiceException.class, () -> generate("bim"));
        request.setModel("scene-chat");
        assertEquals("scene-chat", generate("bim").getScene().get("generatedWith").asText());
        assertEquals("selected-tenant-model", provider.getModelName());
    }

    @Test
    void skipsAShortPrefaceAndClosingRemarksAroundAWholePage() {
        output = "好的，以下是页面：\n```html\n" + HTML + "\n```\n以上页面支持点击摄像头。";
        assertEquals(HTML, generate("bim").getScene().get("html").asText());
        assertEquals("full", service.latest("A", "1", "bim").getScene().get("edit").asText());
    }

    @Test
    void editsTheSavedPageWithSearchReplaceBlocksAndKeepsEverythingElse() {
        generate("bim");
        request.setRevision(1);
        request.setPrompt("保留园区，加一个二层剖面按钮");
        output = "<<<<<<< SEARCH\n<button id='cam'>东门</button>\n=======\n<button id='cam'>东门</button><button id='floor2'>2F 剖面</button>\n>>>>>>> REPLACE\n"
            + "<<<<<<< SEARCH\n<title>园区 &amp; 入口</title>\n=======\n<title>园区剖面</title>\n>>>>>>> REPLACE";
        List<JsonNode> drafts = new ArrayList<>();
        JsonNode scene = service.generate("A", "1", "bim", request, drafts::add).getScene();
        String html = scene.get("html").asText();
        assertTrue(html.contains("<button id='floor2'>2F 剖面</button>"));
        assertTrue(html.contains("window.VLS.preview('c1')"), "untouched script must survive the edit");
        assertEquals("园区剖面", scene.get("title").asText());
        assertEquals("patch", scene.get("edit").asText());
        assertEquals(2, scene.get("edits").asInt());
        assertTrue(drafts.stream().allMatch(draft -> "patch".equals(draft.path("edit").asText())));
        assertTrue(drafts.get(drafts.size() - 1).get("html").asText().contains("floor2"));
        JsonNode log = scene.get("log");
        assertEquals(2, log.size());
        assertEquals("保留园区，加一个二层剖面按钮", log.get(1).get("prompt").asText());
        assertEquals("patch", log.get(1).get("edit").asText());
    }

    @Test
    void anEditThatNoLongerFitsFallsBackToOneWholePageRequest() {
        generate("topology");
        request.setRevision(1);
        String rewritten = HTML.replace("东门", "东门 · 高亮");
        outputs.add("<<<<<<< SEARCH\n<div>不存在的代码</div>\n=======\n<div>新</div>\n>>>>>>> REPLACE");
        outputs.add(rewritten);
        List<JsonNode> drafts = new ArrayList<>();
        JsonNode scene = service.generate("A", "1", "topology", request, drafts::add).getScene();
        assertEquals(rewritten, scene.get("html").asText());
        assertEquals(3, inputs.size());
        assertTrue(inputs.get(2).contains("这次必须输出完整页面"));
        assertTrue(inputs.get(2).contains("<current_page>\n" + HTML));
        assertTrue(drafts.stream().anyMatch(draft -> draft.path("notice").asText().contains("没有在当前页面中找到对应代码")));
        assertEquals(2, service.latest("A", "1", "topology").getRevision());
    }

    @Test
    void undoRestoresThePreviousPageAsANewRevisionAndDropsTheUndoneRequestFromHistory() {
        generate("bim");
        request.setRevision(1);
        request.setPrompt("改成夜景");
        output = HTML.replace("东门", "夜景东门");
        generate("bim");
        VideoSquareSceneService.UndoRequest undo = new VideoSquareSceneService.UndoRequest();
        undo.setRevision(1);
        assertEquals(Integer.valueOf(409), assertThrows(ServiceException.class, () -> service.undo("A", "1", "bim", undo)).getCode());
        undo.setRevision(2);
        VideoSquareSceneService.SavedScene restored = service.undo("A", "1", "bim", undo);
        assertEquals(3, restored.getRevision());
        assertEquals(HTML, restored.getScene().get("html").asText());
        assertEquals("生成交互园区", restored.getPrompt());
        assertEquals(0, restored.getScene().get("undoCount").asInt());
        assertEquals("undo", restored.getScene().at("/log/2/kind").asText());
        undo.setRevision(3);
        assertThrows(ServiceException.class, () -> service.undo("A", "1", "bim", undo));
        request.setRevision(3);
        request.setPrompt("加一个二层剖面");
        output = HTML;
        generate("bim");
        String context = inputs.get(inputs.size() - 1);
        assertTrue(context.contains("1. 生成交互园区\n"));
        assertFalse(context.contains("改成夜景"));
    }

    @Test
    void patchesAreAppliedExactlyOrByUniqueTrimmedLines() {
        String page = "<body>\n  <div id='a'>\n    <b>1</b>\n  </div>\n  <b>1</b>\n</body>";
        String moved = VideoSquareScenePatch.apply(page, new VideoSquareScenePatch.Block("<div id='a'>\n<b>1</b>", "<div id='a'>\n<b>2</b>"), 1);
        assertEquals("<body>\n<div id='a'>\n<b>2</b>\n  </div>\n  <b>1</b>\n</body>", moved);
        assertThrows(VideoSquareScenePatch.Mismatch.class, () ->
            VideoSquareScenePatch.apply(page, new VideoSquareScenePatch.Block("<b>1</b>", "<b>3</b>"), 1));
        assertThrows(VideoSquareScenePatch.Mismatch.class, () ->
            VideoSquareScenePatch.apply(page, new VideoSquareScenePatch.Block("<i>missing</i>", ""), 2));
        String script = "<script>\n  function draw() {\n    const k = state.zoom * 2;\n  }\n</script>";
        assertEquals("<script>\nfunction draw(){ const k = 3; }\n</script>", VideoSquareScenePatch.apply(script,
            new VideoSquareScenePatch.Block("function draw(){\nconst k=state.zoom*2;\n}", "function draw(){ const k = 3; }"), 3),
            "spacing re-flowed by the model still identifies the unique original lines");
        List<VideoSquareScenePatch.Block> blocks = VideoSquareScenePatch.parse(
            "```html\n<<<<<<< SEARCH\na\n=======\n>>>>>>> REPLACE\n```\n<<<<<<< SEARCH\nb\n=======\nc");
        assertEquals(1, blocks.size(), "an unfinished block waits for more output");
        assertEquals("", blocks.get(0).replace);
    }

    @Test
    void aFreshPageIgnoresTheSavedSourceAndHistoryButKeepsItRestorable() {
        request.setDeviceIds(Collections.singletonList("ONVIF:7"));
        when(catalog.page(1)).thenReturn(catalogRows("ONVIF:7"), catalogRows("CAM:2"));
        generate("topology");
        request.setRevision(1);
        request.setFresh(true);
        request.setPrompt("换成环形指挥屏");
        request.setDeviceIds(Collections.singletonList("CAM:2"));
        output = HTML.replace("东门", "环形");
        JsonNode scene = generate("topology").getScene();
        String context = inputs.get(1);
        assertFalse(context.contains("<current_page>"));
        assertFalse(context.contains("历次要求"));
        assertEquals("CAM:2", scene.at("/cameras/c2").asText(), "aliases keep counting so restored pages stay valid");
        assertFalse(scene.get("cameras").has("c1"));
        assertEquals(1, scene.get("undoCount").asInt());
    }
}
