package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudAiService {
    public static final Set<String> TERMINAL = new HashSet<>(Arrays.asList("COMPLETED", "CANCELLED", "FAILED", "SKIPPED"));
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ComputeNodeService nodes;
    private final ConfiguredComputeRegistry configured;
    private final ComputeSharedQueueService queue;
    private final CloudAiSources sources;
    private final CloudAiModels models;

    public static String tenant() {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty()) throw new ServiceException("缺少可信租户上下文", 403);
        return tenant;
    }
    public static String actor() {
        String actor = LoginHelper.getUserId();
        if (actor == null || actor.trim().isEmpty()) throw new ServiceException("请先登录", 401);
        return actor;
    }
    public List<Map<String,Object>> nodeChoices() {
        actor(); List<ComputeNode> all = new ArrayList<>(configured.published()); all.addAll(nodes.list());
        List<Map<String,Object>> result = new ArrayList<>(); Set<Long> seen = new HashSet<>();
        for (ComputeNode node : all) {
            if (!freeSelf(node) || node.isConfigurationMigrationPending() || node.isConfigurationReplaced()
                    || !seen.add(configured.canonicalId(node.getId())) || !queue.isShared(node) && !nodes.canUsePrivate(node)) continue;
            Map<String,Object> item = new LinkedHashMap<>(); item.put("id", String.valueOf(node.getId())); item.put("name", node.getName());
            item.put("gpuModel", node.getGpuModel()); item.put("state", node.getProbeState()); item.put("enabled", node.isEnabled());
            item.put("shared", queue.isShared(node)); result.add(item);
        }
        return result;
    }
    public List<ObjectNode> modelChoices(Long nodeId) { return models.choices(usable(nodeId, false)); }
    private static boolean freeSelf(ComputeNode node) {
        return !"POINTS".equals(node.getBillingType()) && "self".equalsIgnoreCase(node.getProvider());
    }
    private ComputeNode usable(Long nodeId, boolean ready) {
        if (nodeId == null) throw new ServiceException("请选择自有GPU");
        ComputeNode node = nodes.get(nodeId);
        if (!freeSelf(node)) throw new ServiceException("本期云端检测仅使用自有／公司GPU");
        if (!queue.isShared(node) && !nodes.canUsePrivate(node)) throw new ServiceException("无权使用此GPU", 403);
        if (queue.isShared(node) && queue.viewer().isEmpty()) throw new ServiceException("缺少共享GPU执行身份", 403);
        return ready ? nodes.requireReady(nodeId) : node;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> saveScene(String id, CloudAiConfiguration input) {
        requireName(input.name); if (input.description != null && input.description.length() > 1000) throw new ServiceException("说明最多1000字");
        if (input.cameras == null || input.cameras.isEmpty() || input.cameras.size() > 32) throw new ServiceException("请选择1至32路摄像机");
        if (input.models == null || input.models.isEmpty() || input.models.size() > 16) throw new ServiceException("请选择1至16个算法模型");
        if (input.preSeconds < 0 || input.preSeconds > 60 || input.postSeconds < 1 || input.postSeconds > 120)
            throw new ServiceException("录像前段支持0至60秒，后段支持1至120秒");
        if (!Double.isFinite(input.sampleFps) || input.sampleFps < 0.2 || input.sampleFps > 30) throw new ServiceException("检测频率须在0.2至30帧/秒之间");
        ComputeNode node = usable(input.nodeId, true);
        ObjectNode snapshot = json.valueToTree(input); snapshot.put("nodeId", String.valueOf(node.getId())); snapshot.put("nodeName", node.getName());
        snapshot.set("resource", json.valueToTree(configured.snapshot(node)));
        snapshot.put("queueOwner", queue.isShared(node) ? queue.viewer() : "");
        snapshot.put("shared", queue.isShared(node));
        snapshot.put("managementOwner", node.getManagementOwnerSubject());
        ArrayNode cameras = json.createArrayNode(), rules = json.createArrayNode(); Set<String> identities = new HashSet<>(), algorithms = new HashSet<>();
        for (String grantId : input.cameras) {
            JsonNode source = sources.owned(grantId);
            String identity = source.path("protocol").asText() + ":" + source.path("deviceRowId").asText() + ":" + source.path("streamId").asText();
            if (!identities.add(identity)) throw new ServiceException("不能重复选择相同摄像机视频源");
            cameras.add(source);
        }
        for (CloudAiConfiguration.ModelRule rule : input.models) {
            if (!Double.isFinite(rule.confidence) || rule.confidence <= 0 || rule.confidence > 1 || rule.reportInterval < 1 || rule.reportInterval > 86400)
                throw new ServiceException("置信度须大于0且不超过1，上报间隔须为1至86400秒");
            ObjectNode fixed = models.snapshot(rule.modelId, node);
            if (!algorithms.add(fixed.path("algorithmId").asText())) throw new ServiceException("同一算法只选择一个固定模型版本");
            if (rule.classes == null) rule.classes = new ArrayList<>();
            for (Integer category : rule.classes) if (category == null || !fixed.path("labels").has(String.valueOf(category))) throw new ServiceException("告警类别不属于所选模型");
            fixed.put("confidence", rule.confidence); fixed.put("reportInterval", rule.reportInterval); fixed.set("classes", json.valueToTree(new TreeSet<>(rule.classes)));
            rules.add(fixed);
        }
        snapshot.set("cameras", cameras); snapshot.set("models", rules);
        if (id == null) {
            id = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO vls_cloud_ai_scene(id,tenant_id,actor_id,name,description,config_json,enabled) VALUES(?,?,?,?,?,?,?)",
                id, tenant(), actor(), input.name.trim(), input.description, snapshot.toString(), input.enabled);
        } else {
            Map<String,Object> old = owned("scene", id, true);
            if (input.revision == null || input.revision.intValue() != ((Number) old.get("revision")).intValue()) throw new ServiceException("场景已被更新，请刷新后编辑", 409);
            jdbc.update("UPDATE vls_cloud_ai_scene SET name=?,description=?,config_json=?,enabled=?,revision=revision+1 WHERE id=? AND tenant_id=?",
                input.name.trim(), input.description, snapshot.toString(), input.enabled, id, tenant());
        }
        return scene(id);
    }
    public Map<String,Object> scene(String id) { return decorate(owned("scene", id, false)); }
    public Map<String,Object> task(String id) { return decorate(owned("task", id, false)); }
    public Map<String,Object> run(String id) { return decorate(owned("run", id, false)); }

    @Transactional
    public void sceneEnabled(String id, boolean enabled) {
        Map<String,Object> row = owned("scene", id, true);
        if (enabled && read(String.valueOf(row.get("config_json"))).path("models").size() == 0) throw new ServiceException("请先补全场景模型与摄像机配置");
        jdbc.update("UPDATE vls_cloud_ai_scene SET enabled=?,revision=revision+1 WHERE id=? AND tenant_id=?", enabled, id, tenant());
    }
    @Transactional
    public void deleteScene(String id) {
        owned("scene", id, true);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_ai_task WHERE scene_id=? AND tenant_id=? AND deleted=0", Integer.class, id, tenant());
        if (count != null && count > 0) throw new ServiceException("该场景仍被任务引用，请先删除任务配置；历史运行会保留");
        jdbc.update("UPDATE vls_cloud_ai_scene SET deleted=1 WHERE id=? AND tenant_id=?", id, tenant());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> saveTask(String id, CloudAiConfiguration.Task input) {
        requireName(input.name); CloudAiSchedule schedule;
        try { schedule = new CloudAiSchedule(input.schedule); } catch (IllegalArgumentException error) { throw new ServiceException(error.getMessage()); }
        Map<String,Object> scene = owned("scene", input.sceneId, true);
        if (!flag(scene.get("enabled"))) throw new ServiceException("场景尚未启用，请先完善并启用场景");
        JsonNode snapshot = read(String.valueOf(scene.get("config_json")));
        usable(snapshot.path("nodeId").asLong(), true);
        if (snapshot.path("models").size() == 0) throw new ServiceException("场景尚未配置检测模型");
        CloudAiSchedule.Window next = schedule.currentOrNext(Instant.now());
        if (schedule.kind() != CloudAiSchedule.Kind.MANUAL && next == null) throw new ServiceException("时间规则已过期，请选择未来的执行窗口");
        if (id == null) {
            id = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO vls_cloud_ai_task(id,tenant_id,actor_id,scene_id,name,schedule_json,enabled,next_start,next_end) VALUES(?,?,?,?,?,?,?,?,?)",
                id, tenant(), actor(), input.sceneId, input.name.trim(), write(input.schedule), input.enabled,
                next == null ? null : next.start.toEpochMilli(), next == null ? null : next.end.toEpochMilli());
            if (schedule.kind() == CloudAiSchedule.Kind.MANUAL) {
                Map<String,Object> task = owned("task", id, false);
                createRun(task, scene, "manual", System.currentTimeMillis(), null, schedule.durationSeconds(), "QUEUED", "等待GPU调度");
                jdbc.update("UPDATE vls_cloud_ai_task SET enabled=0 WHERE id=?", id);
            }
        } else {
            Map<String,Object> old = owned("task", id, true);
            if ("MANUAL".equals(read(String.valueOf(old.get("schedule_json"))).path("kind").asText()) || schedule.kind() == CloudAiSchedule.Kind.MANUAL)
                throw new ServiceException("手工任务一经创建即触发；再次执行请新建任务");
            if (input.revision == null || input.revision.intValue() != ((Number) old.get("revision")).intValue()) throw new ServiceException("任务已更新，请刷新后编辑", 409);
            jdbc.update("UPDATE vls_cloud_ai_task SET scene_id=?,name=?,schedule_json=?,enabled=?,next_start=?,next_end=?,revision=revision+1 WHERE id=? AND tenant_id=?",
                input.sceneId, input.name.trim(), write(input.schedule), input.enabled, next.start.toEpochMilli(), next.end.toEpochMilli(), id, tenant());
        }
        return task(id);
    }
    @Transactional
    public void taskEnabled(String id, boolean enabled) {
        Map<String,Object> task = owned("task", id, true);
        CloudAiSchedule schedule = schedule(String.valueOf(task.get("schedule_json")));
        if (schedule.kind() == CloudAiSchedule.Kind.MANUAL) throw new ServiceException("手工任务不能作为定时配置启停");
        CloudAiSchedule.Window next = schedule.currentOrNext(Instant.now());
        if (enabled && next == null) throw new ServiceException("任务时间规则已过期，请先修改时间");
        jdbc.update("UPDATE vls_cloud_ai_task SET enabled=?,next_start=?,next_end=?,revision=revision+1 WHERE id=? AND tenant_id=?", enabled,
            next == null ? null : next.start.toEpochMilli(), next == null ? null : next.end.toEpochMilli(), id, tenant());
    }
    @Transactional
    public void deleteTask(String id) {
        owned("task", id, true);
        Integer active = jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_ai_run WHERE task_id=? AND state NOT IN ('COMPLETED','CANCELLED','FAILED','SKIPPED')", Integer.class, id);
        if (active != null && active > 0) throw new ServiceException("请先停止该任务的活动运行，再删除配置");
        jdbc.update("UPDATE vls_cloud_ai_task SET deleted=1,enabled=0 WHERE id=? AND tenant_id=?", id, tenant());
    }
    public void stop(String id) {
        Map<String,Object> run = owned("run", id, false);
        if (!TERMINAL.contains(String.valueOf(run.get("state"))))
            jdbc.update("UPDATE vls_cloud_ai_run SET cancel_requested=1,message='已请求停止，等待执行器确认退出' WHERE id=? AND tenant_id=?", id, tenant());
    }

    public Map<String,Object> page(String type, int page, int size, String search, String state, String sceneId) {
        if (!Arrays.asList("scene", "task", "run").contains(type)) throw new ServiceException("查询类型无效");
        size = Math.max(1, Math.min(size, 100)); page = Math.max(1, page);
        List<Object> args = new ArrayList<>(); args.add(tenant()); args.add(actor());
        String where = " WHERE tenant_id=? AND actor_id=?" + ("run".equals(type) ? "" : " AND deleted=0");
        if (search != null && !search.trim().isEmpty()) {
            where += " AND " + ("run".equals(type) ? "(CONVERT(id USING utf8mb4) LIKE ? OR snapshot_json LIKE ?)" : "name LIKE ?");
            args.add("%" + search.trim() + "%"); if ("run".equals(type)) args.add("%" + search.trim() + "%");
        }
        if ("run".equals(type) && state != null && !state.isEmpty()) { where += " AND state=?"; args.add(state); }
        if (!"scene".equals(type) && sceneId != null && !sceneId.isEmpty()) { where += " AND scene_id=?"; args.add(sceneId); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_ai_" + type + where, Long.class, args.toArray());
        args.add(size); args.add((page - 1L) * size);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM vls_cloud_ai_" + type + where + " ORDER BY create_time DESC,id DESC LIMIT ? OFFSET ?", args.toArray());
        rows.forEach(this::decorate); Map<String,Object> result = new LinkedHashMap<>(); result.put("records", rows); result.put("total", total); return result;
    }
    Map<String,Object> owned(String type, String id, boolean lock) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM vls_cloud_ai_" + type + " WHERE id=? AND tenant_id=? AND actor_id=?"
            + ("run".equals(type) ? "" : " AND deleted=0") + (lock ? " FOR UPDATE" : ""), id, tenant(), actor());
        if (rows.isEmpty()) throw new ServiceException("记录不存在或无权访问", 403); return rows.get(0);
    }
    Map<String,Object> decorate(Map<String,Object> row) {
        if (row.containsKey("config_json")) row.put("config", publicSnapshot(read(String.valueOf(row.remove("config_json")))));
        if (row.containsKey("schedule_json")) row.put("schedule", read(String.valueOf(row.remove("schedule_json"))));
        if (row.containsKey("snapshot_json")) row.put("snapshot", publicSnapshot(read(String.valueOf(row.remove("snapshot_json")))));
        if (row.get("detail_json") != null) row.put("detail", read(String.valueOf(row.remove("detail_json"))));
        row.remove("lease_owner"); row.remove("lease_until"); row.remove("run_dir"); row.remove("actor_id"); row.remove("tenant_id");
        return row;
    }
    private JsonNode publicSnapshot(JsonNode source) {
        ObjectNode safe = ((ObjectNode) source).deepCopy(); safe.remove(Arrays.asList("resource", "queueOwner", "sharedQueue", "managementOwner"));
        for (JsonNode model : safe.path("models")) ((ObjectNode) model).remove(Arrays.asList("path", "sha256", "size")); return safe;
    }
    String createRun(Map<String,Object> task, Map<String,Object> scene, String key, long start, Long end, Integer duration, String state, String message) {
        String id = UUID.randomUUID().toString(); ObjectNode snapshot = (ObjectNode) read(String.valueOf(scene.get("config_json")));
        snapshot.put("sceneName", String.valueOf(scene.get("name"))); snapshot.put("sceneRevision", ((Number) scene.get("revision")).intValue());
        snapshot.put("taskName", String.valueOf(task.get("name")));
        jdbc.update("INSERT IGNORE INTO vls_cloud_ai_run(id,tenant_id,actor_id,task_id,scene_id,window_key,planned_start,planned_end,duration_seconds,snapshot_json,node_id,state,message,ended_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id, task.get("tenant_id"), task.get("actor_id"), task.get("id"), scene.get("id"), key, start, end, duration, snapshot.toString(), snapshot.path("nodeId").asLong(), state, message, TERMINAL.contains(state) ? System.currentTimeMillis() : null);
        return id;
    }
    JsonNode read(String value) { try { return json.readTree(value); } catch (Exception error) { throw new ServiceException("云端检测配置格式异常"); } }
    String write(Object value) { try { return json.writeValueAsString(value); } catch (Exception error) { throw new ServiceException("配置序列化失败"); } }
    CloudAiSchedule schedule(String value) {
        try { return new CloudAiSchedule(json.readValue(value, CloudAiSchedule.Definition.class)); }
        catch (Exception error) { throw new ServiceException("任务时间规则无效"); }
    }
    static boolean flag(Object value) { return Boolean.TRUE.equals(value) || value instanceof Number && ((Number) value).intValue() != 0; }
    private static void requireName(String name) { if (name == null || name.trim().isEmpty() || name.length() > 100) throw new ServiceException("请填写1至100字名称"); }
}
