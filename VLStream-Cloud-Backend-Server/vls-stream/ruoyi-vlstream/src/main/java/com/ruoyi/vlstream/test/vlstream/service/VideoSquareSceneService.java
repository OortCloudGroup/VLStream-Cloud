package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.ruoyi.vlstream.test.vlstream.service.VideoSquareSceneFormat.JSON;

@Service
@RequiredArgsConstructor
public class VideoSquareSceneService {
    private static final String OWNER = " WHERE tenant_id=? AND owner_id=? AND view_mode=?";
    /** Conversation turns kept for restoring the chat and giving the model the feature history. */
    private static final int LOG_LIMIT = 30;
    private static final int HISTORY_FOR_MODEL = 8;
    /** Earlier pages kept for "undo". Each is at most 128 KB, so the row stays far below MEDIUMTEXT. */
    private static final int UNDO_LIMIT = 5;
    private static final String UNDO_PROMPT = "回退到上一版";
    private final JdbcTemplate jdbc;
    private final AuthorizedVideoCatalog catalog;
    private final LlmSystemProviderService providers;
    private final OpenAiVisionClient client;

    public ModelChoices models() {
        LlmProvider provider = providers.requireAuthorized();
        return new ModelChoices(provider.getModelName(), client.listSceneModels(provider));
    }

    /** The current page for the browser. Undo snapshots stay on the server. */
    public SavedScene latest(String tenant, String owner, String mode) {
        SavedScene stored = load(tenant, owner, mode);
        return new SavedScene(stored.revision, stored.prompt, visible(stored.scene));
    }

    private SavedScene load(String tenant, String owner, String mode) {
        VideoSquareSceneFormat.requireMode(mode);
        List<SavedScene> rows = jdbc.query("SELECT revision,prompt,scene_json FROM vls_video_square_scene" + OWNER,
            (rs, row) -> {
                try {
                    return new SavedScene(rs.getInt("revision"), rs.getString("prompt"), JSON.readTree(rs.getString("scene_json")));
                } catch (JsonProcessingException ex) {
                    throw new ServiceException("已保存的视图无法读取");
                }
            }, tenant, owner, mode);
        return rows.isEmpty() ? new SavedScene(0, "", null) : rows.get(0);
    }

    public SavedScene generate(String tenant, String owner, String mode, GenerateRequest request, Consumer<ObjectNode> preview) {
        VideoSquareSceneFormat.requireMode(mode);
        if (request == null || StringUtils.isBlank(request.prompt) || request.prompt.length() > 2000) {
            throw new ServiceException("请输入 1 至 2000 字的视图描述", 400);
        }
        SavedScene previous = load(tenant, owner, mode);
        if (request.revision == null || request.revision < 0 || request.revision != previous.revision) throw conflict();
        List<String> ids = request.deviceIds == null ? Collections.emptyList() : request.deviceIds;
        if (ids.size() > 80 || ids.stream().anyMatch(id -> StringUtils.isBlank(id) || id.length() > 160)) {
            throw new ServiceException("本次可关联的设备列表无效", 400);
        }
        String prompt = request.prompt.trim();
        // A fresh page ignores the saved source and history; the saved page stays restorable through undo.
        boolean fresh = Boolean.TRUE.equals(request.fresh);
        boolean page = previous.scene != null && VideoSquareSceneFormat.FORMAT.equals(previous.scene.path("format").asText());
        Set<String> wanted = new HashSet<>(ids);
        Map<String,String> previousAliases = new HashMap<>();
        int lastSerial = 0;
        if (page) {
            previous.scene.path("cameras").fields().forEachRemaining(entry ->
                previousAliases.put(entry.getValue().asText(), entry.getKey()));
            if (!fresh) wanted.addAll(previousAliases.keySet());
            lastSerial = previous.scene.path("cameraSerial").asInt();
        } else if (previous.scene != null && !fresh) {
            wanted.addAll(previous.scene.findValuesAsText("deviceId"));
        }
        Map<String,String> names = new LinkedHashMap<>();
        ObjectNode bindings = JSON.createObjectNode();
        Map<String,String> currentAliases = new HashMap<>();
        for (Map.Entry<String,String> entry : authorizedCameras(wanted).entrySet()) {
            String id = entry.getKey();
            String alias = previousAliases.get(id);
            // Never recycle a removed alias: user-generated scripts may still reference it.
            if (alias == null) alias = "c" + (++lastSerial);
            String name = entry.getValue();
            names.put(alias, StringUtils.isBlank(name) || id.equals(name) ? alias : name);
            bindings.put(alias, id);
            currentAliases.put(id, alias);
        }
        String currentPage = page && !fresh ? previous.scene.path("html").asText(null) : null;
        JsonNode legacy = null;
        if (previous.scene != null && !page && !fresh) {
            ObjectNode context = previous.scene.deepCopy();
            context.remove(Arrays.asList("log", "undo"));
            replaceBindings(context, currentAliases);
            legacy = context;
        }
        List<String> history = fresh ? Collections.<String>emptyList() : history(previous);
        LlmProvider provider = providers.requireAuthorized();
        String model = StringUtils.trimToNull(request.model);
        if (model == null) model = provider.getModelName();
        else if (model.length() > 200 || !client.listSceneModels(provider).contains(model)) {
            throw new ServiceException("所选模型已不在当前 OortCloud 授权目录中，请重新选择", 422);
        }
        if (StringUtils.isBlank(model)) throw new ServiceException("请为本次视图选择一个 OortCloud 模型");

        String instructions = VideoSquareSceneFormat.instructions(mode);
        ObjectNode scene;
        VideoSquareSceneStream stream = new VideoSquareSceneStream(mode, currentPage, preview);
        try {
            client.generateScene(provider, model, instructions,
                VideoSquareSceneFormat.input(mode, prompt, names, history, currentPage, legacy, null), stream::accept);
            scene = stream.finish();
        } catch (VideoSquareScenePatch.Mismatch mismatch) {
            // One whole-page attempt keeps the request reliable when an edit no longer fits the saved code.
            String note = mismatch.getMessage() + "，正在改为重写完整页面并保留已有功能";
            preview.accept(JSON.createObjectNode().put("format", VideoSquareSceneFormat.FORMAT)
                .put("title", previous.scene.path("title").asText(""))
                .put("html", currentPage).put("edit", VideoSquareSceneStream.FULL).put("notice", note));
            VideoSquareSceneStream whole = new VideoSquareSceneStream(mode, null, preview);
            client.generateScene(provider, model, instructions, VideoSquareSceneFormat.input(mode, prompt, names, history,
                currentPage, null, "上一次输出的修改片段无法对应当前页面（" + mismatch.getMessage()
                    + "）。这次必须输出完整页面（格式 A），在当前页面基础上实现本次要求，并保留全部已有功能。"), whole::accept);
            scene = whole.finish();
        }
        scene.remove("chars");
        scene.set("cameras", bindings);
        scene.put("cameraSerial", lastSerial);
        scene.put("generatedWith", model);
        scene.set("log", appendLog(previous.scene, previous.revision + 1, "generate", prompt, scene));
        scene.set("undo", pushUndo(previous));
        int revision = save(tenant, owner, mode, previous.revision, prompt, scene);
        return new SavedScene(revision, prompt, visible(scene));
    }

    /** Restores the page before the latest change as a new revision, so other tabs still detect the update. */
    public SavedScene undo(String tenant, String owner, String mode, UndoRequest request) {
        SavedScene current = load(tenant, owner, mode);
        if (request == null || request.revision == null || request.revision != current.revision || current.scene == null) {
            throw conflict();
        }
        JsonNode stack = current.scene.path("undo");
        if (!stack.isArray() || stack.size() == 0) throw new ServiceException("没有可以回退的上一版", 400);
        ObjectNode scene = (ObjectNode) stack.get(0).deepCopy();
        String prompt = scene.path("_prompt").asText("");
        scene.remove(Arrays.asList("_prompt", "_revision"));
        // Aliases must stay unique across every version that might be restored later.
        scene.put("cameraSerial", Math.max(scene.path("cameraSerial").asInt(), current.scene.path("cameraSerial").asInt()));
        ArrayNode rest = JSON.createArrayNode();
        for (int i = 1; i < stack.size(); i++) rest.add(stack.get(i));
        scene.set("log", appendLog(current.scene, current.revision + 1, "undo", UNDO_PROMPT, scene));
        scene.set("undo", rest);
        int revision = save(tenant, owner, mode, current.revision, prompt, scene);
        return new SavedScene(revision, prompt, visible(scene));
    }

    private int save(String tenant, String owner, String mode, int revision, String prompt, ObjectNode scene) {
        // Publish a complete result atomically. A failed or stale request leaves the previous view intact.
        if (revision == 0) {
            try {
                jdbc.update("INSERT INTO vls_video_square_scene (tenant_id,owner_id,view_mode,revision,prompt,scene_json) VALUES (?,?,?,1,?,?)",
                    tenant, owner, mode, prompt, scene.toString());
            } catch (DuplicateKeyException ex) {
                throw conflict();
            }
        } else if (jdbc.update("UPDATE vls_video_square_scene SET revision=revision+1,prompt=?,scene_json=?" + OWNER + " AND revision=?",
            prompt, scene.toString(), tenant, owner, mode, revision) != 1) {
            throw conflict();
        }
        return revision + 1;
    }

    /** Requests still reflected in the current page, oldest first. Undone turns are dropped. */
    static List<String> history(SavedScene previous) {
        List<String> effective = new ArrayList<>();
        if (previous.scene == null) return effective;
        JsonNode log = previous.scene.path("log");
        if (!log.isArray() || log.size() == 0) {
            if (StringUtils.isNotBlank(previous.prompt)) effective.add(previous.prompt);
            return effective;
        }
        for (JsonNode entry : log) {
            if ("undo".equals(entry.path("kind").asText())) {
                if (!effective.isEmpty()) effective.remove(effective.size() - 1);
            } else if (StringUtils.isNotBlank(entry.path("prompt").asText())) {
                effective.add(entry.path("prompt").asText());
            }
        }
        return effective.subList(Math.max(0, effective.size() - HISTORY_FOR_MODEL), effective.size());
    }

    private static ArrayNode appendLog(JsonNode previous, int revision, String kind, String prompt, JsonNode scene) {
        ArrayNode log = JSON.createArrayNode();
        JsonNode earlier = previous == null ? null : previous.path("log");
        if (earlier != null && earlier.isArray()) {
            for (int i = Math.max(0, earlier.size() - LOG_LIMIT + 1); i < earlier.size(); i++) log.add(earlier.get(i));
        }
        ObjectNode entry = log.addObject().put("revision", revision).put("kind", kind).put("prompt", prompt)
            .put("title", scene.path("title").asText("")).put("at", System.currentTimeMillis());
        if (scene.hasNonNull("generatedWith") && "generate".equals(kind)) entry.put("model", scene.path("generatedWith").asText());
        if (scene.hasNonNull("edit") && "generate".equals(kind)) entry.put("edit", scene.path("edit").asText());
        if (scene.hasNonNull("edits") && "generate".equals(kind)) entry.put("edits", scene.path("edits").asInt());
        return log;
    }

    private static ArrayNode pushUndo(SavedScene previous) {
        ArrayNode stack = JSON.createArrayNode();
        if (previous.scene == null) return stack;
        ObjectNode snapshot = previous.scene.deepCopy();
        snapshot.remove(Arrays.asList("log", "undo"));
        snapshot.put("_prompt", previous.prompt).put("_revision", previous.revision);
        stack.add(snapshot);
        JsonNode earlier = previous.scene.path("undo");
        if (earlier.isArray()) for (int i = 0; i < earlier.size() && stack.size() < UNDO_LIMIT; i++) stack.add(earlier.get(i));
        return stack;
    }

    private static JsonNode visible(JsonNode scene) {
        if (scene == null || !scene.isObject()) return scene;
        ObjectNode copy = scene.deepCopy();
        JsonNode undo = copy.remove("undo");
        copy.put("undoCount", undo != null && undo.isArray() ? undo.size() : 0);
        return copy;
    }

    private static void replaceBindings(JsonNode node, Map<String,String> aliasesById) {
        if (node.isObject() && node.has("deviceId")) {
            ObjectNode object = (ObjectNode) node;
            String id = object.remove("deviceId").asText();
            object.put("camera", aliasesById.get(id));
        }
        if (node.isContainerNode()) node.forEach(child -> replaceBindings(child, aliasesById));
    }

    private Map<String, String> authorizedCameras(Set<String> wanted) {
        Map<String, String> names = new LinkedHashMap<>();
        // Device IDs from the browser narrow the directory; only WVP can supply names or grant access.
        for (int page = 1; !wanted.isEmpty(); page++) {
            JsonNode data = catalog.page(page);
            for (JsonNode device : data.path("rows")) {
                String id = device.path("id").asText();
                if (!wanted.remove(id)) continue;
                names.put(id, StringUtils.abbreviate(device.path("deviceName").asText("摄像头"), 80));
            }
            if (data.path("rows").size() == 0 || (long)page * AuthorizedVideoCatalog.PAGE_SIZE >= data.path("total").asLong()) break;
        }
        return names;
    }

    private static ServiceException conflict() {
        return new ServiceException("此视图已在其他页面更新，请重新载入后再生成", 409);
    }

    @Data
    public static class GenerateRequest {
        private String prompt;
        private String model;
        private Integer revision;
        private List<String> deviceIds;
        /** Design a new page instead of editing the saved one. */
        private Boolean fresh;
    }

    @Data
    public static class UndoRequest {
        private Integer revision;
    }

    @Value
    public static class ModelChoices {
        String defaultModel;
        List<String> models;
    }

    @Value
    public static class SavedScene {
        int revision;
        String prompt;
        JsonNode scene;
    }
}
