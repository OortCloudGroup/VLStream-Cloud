package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import cn.hutool.core.util.IdUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudAiEvents {
    private final JdbcTemplate jdbc;
    private final CloudAiService service;
    private final VlsEventReportApplicationService application;
    private final IVlsEventManagementService events;
    private final DeviceMediaUploadService media;
    private final CloudAiLocalEventSink localEvents;

    @Transactional(rollbackFor = Exception.class)
    public void ingest(Map<String,Object> run, JsonNode report, Map<String,String> evidence) {
        String tenant = String.valueOf(run.get("tenant_id")), runId = String.valueOf(run.get("id")), id = report.path("id").asText();
        if (!tenant.equals(CloudAiService.tenant()) || !id.matches("[a-f0-9-]{36}")) throw new ServiceException("检测事件身份无效");
        JsonNode snapshot = service.read(String.valueOf(run.get("snapshot_json")));
        JsonNode camera = member(snapshot.path("cameras"), "grantId", report.path("cameraKey").asText());
        JsonNode model = member(snapshot.path("models"), "modelId", report.path("modelId").asText());
        long detectedAt = report.path("detectedAt").asLong();
        if (detectedAt < ((Number) run.get("planned_start")).longValue() - 60000 || detectedAt > System.currentTimeMillis() + 60000
                || !report.path("detections").isArray() || report.path("detections").isEmpty() || report.path("detections").size() > 300) throw new ServiceException("检测事件时间或目标数据无效");
        for (JsonNode detection : report.path("detections")) {
            double score = detection.path("confidence").asDouble(-1);
            if (!Double.isFinite(score) || score < 0 || score > 1 || !model.path("labels").has(detection.path("classId").asText())) throw new ServiceException("检测类别或置信度无效");
        }
        // Serialize ingestion on the durable run, so even expired worker leases cannot duplicate an event.
        jdbc.queryForList("SELECT id FROM vls_cloud_ai_run WHERE id=? AND tenant_id=? FOR UPDATE", runId, tenant);
        List<Map<String,Object>> existing = jdbc.queryForList("SELECT * FROM vls_cloud_ai_event WHERE id=? FOR UPDATE", id);
        DeviceInfo device = new DeviceInfo(); device.setTenantId(tenant); device.setDeviceId(eventDeviceId(camera));
        device.setDeviceName(camera.path("name").asText()); device.setAddress(camera.path("address").asText(""));
        EventManagement event;
        if (existing.isEmpty()) {
            if (!evidence.containsKey("original") || !evidence.containsKey("annotated")) throw new ServiceException("事件截图尚未完成归档");
            event = new EventManagement(); event.setMqttMessageId("cloud-ai:" + id); event.setDeviceEventId(id);
            event.setMediaId(evidence.get("annotated")); event.setReportImg(VlsEventReportApplicationService.mediaReference(evidence.get("annotated")));
            String label = model.path("labels").path(report.path("detections").get(0).path("classId").asText()).asText();
            event.setEventType(label.length() > 80 ? label.substring(0, 80) : label);
            event.setEventDesc(snapshot.path("sceneName").asText() + " · " + camera.path("name").asText() + " · " + model.path("name").asText());
            event.setReportDevice(device.getDeviceId()); event.setReportLocation(device.getAddress()); event.setReportTime(new Date(detectedAt));
            ObjectNode data = ((ObjectNode) report).deepCopy(); data.put("source", "CLOUD_AI"); data.put("runId", runId); data.put("sceneId", String.valueOf(run.get("scene_id")));
            ObjectNode cameraReference = data.putObject("cameraReference");
            for (String field : Arrays.asList("protocol", "deviceRowId", "deviceId", "streamId", "channelId", "channelName")) if (camera.hasNonNull(field)) cameraReference.set(field,camera.get(field));
            event.setEventData(data.toString());
            event.setId(IdUtil.getSnowflakeNextId());
            if (application.isMultiTenant()) application.persistCloudAiEvent(event, device);
            else localEvents.create(tenant, event.getId(), ActiveSafetyEventReport.builder()
                .sourceMessageId(event.getMqttMessageId()).deviceEventId(id)
                .deviceId(device.getDeviceId()).deviceName(device.getDeviceName())
                .eventType(event.getEventType()).description(event.getEventDesc()).eventTime(event.getReportTime())
                .mediaId(evidence.get("annotated")).imageMediaIds(Arrays.asList(evidence.get("annotated"), evidence.get("original")))
                .videoMediaIds(Collections.emptyList()).mqttPayloadJson(data.toString()).address(device.getAddress()).build());
            jdbc.update("INSERT INTO vls_cloud_ai_event(id,tenant_id,run_id,scene_id,event_id,event_sink,camera_key,model_id,detected_at,detail_json,original_media_id,annotated_media_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                id, tenant, runId, run.get("scene_id"), event.getId(), application.isMultiTenant() ? "VLS" : "LOCAL", report.path("cameraKey").asText(), report.path("modelId").asText(), detectedAt, report.toString(), evidence.get("original"), evidence.get("annotated"));
        } else {
            Map<String,Object> before = existing.get(0);
            if (!tenant.equals(before.get("tenant_id")) || !runId.equals(before.get("run_id")) || !report.path("cameraKey").asText().equals(before.get("camera_key")) || !report.path("modelId").asText().equals(before.get("model_id"))) throw new ServiceException("重复事件身份不一致");
            if (!"PENDING".equals(before.get("media_state"))) return;
            long eventId = ((Number) before.get("event_id")).longValue();
            if (application.isMultiTenant() != "VLS".equals(before.get("event_sink"))) throw new ServiceException("事件所属的租户运行模式已改变");
            if (application.isMultiTenant()) event = events.getById(eventId);
            else { event = new EventManagement(); event.setId(eventId); }
        }
        String state = report.path("mediaState").asText("PENDING");
        if (!Arrays.asList("READY", "PARTIAL", "FAILED").contains(state)) return;
        if (!"FAILED".equals(state) && !evidence.containsKey("video")) throw new ServiceException("告警录像尚未完成归档");
        jdbc.update("UPDATE vls_cloud_ai_event SET media_state=?,video_media_id=?,detail_json=? WHERE id=? AND tenant_id=?", state, evidence.get("video"), report.toString(), id, tenant);
        List<String> images = Arrays.asList(evidence.get("annotated"), evidence.get("original"));
        List<String> videos = evidence.containsKey("video") ? Collections.singletonList(evidence.get("video")) : Collections.emptyList();
        if (application.isMultiTenant()) {
            if (event != null) application.publishCloudAiEvidence(event, device, images, videos);
        } else localEvents.completeEvidence(tenant, event.getId(), images, videos);
    }

    public Map<String,Object> page(int page, int size, String search, String status, String sceneId, String runId) {
        CloudAiService.actor(); size = Math.max(1, Math.min(100,size));
        List<Object> args = new ArrayList<>(); args.add(CloudAiService.tenant());
        String where = visibleWhere();
        if (status != null && !status.isEmpty()) { where += " AND " + statusExpression() + "=?"; args.add(status); }
        if (sceneId != null && !sceneId.isEmpty()) { where += " AND c.scene_id=?"; args.add(sceneId); }
        if (runId != null && !runId.isEmpty()) { where += " AND c.run_id=?"; args.add(runId); }
        if (search != null && !search.trim().isEmpty()) { where += " AND (COALESCE(e.event_type,l.item) LIKE ? OR COALESCE(e.event_desc,JSON_UNQUOTE(JSON_EXTRACT(l.data,'$.describe'))) LIKE ? OR COALESCE(e.report_device,l.device_id) LIKE ?)"; for(int i=0;i<3;i++) args.add("%"+search.trim()+"%"); }
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from() + where, Long.class, args.toArray());
        args.add(size); args.add((Math.max(1,page)-1L)*size);
        List<Map<String,Object>> rows = jdbc.queryForList(select() + where + " ORDER BY c.detected_at DESC,c.id DESC LIMIT ? OFFSET ?", args.toArray());
        for (Map<String,Object> row : rows) decorate(row, false);
        Map<String,Object> result = new LinkedHashMap<>(); result.put("records",rows); result.put("total",total); return result;
    }
    public Map<String,Object> detail(Long eventId) {
        CloudAiService.actor();
        List<Map<String,Object>> rows = jdbc.queryForList(select() + visibleWhere() + " AND c.event_id=?", CloudAiService.tenant(), eventId);
        if (rows.isEmpty()) throw new ServiceException("云端事件不存在或无权访问", 403);
        return decorate(rows.get(0), true);
    }
    private String from() {
        return " FROM vls_cloud_ai_event c"
            + " LEFT JOIN vls_event_management e ON c.event_sink='VLS' AND e.id=c.event_id AND e.tenant_id=c.tenant_id"
            + " LEFT JOIN oort_task_event l ON c.event_sink='LOCAL' AND l.id=CAST(c.event_id AS CHAR) COLLATE utf8mb4_general_ci AND l.no=0 AND l.tenant_id=c.tenant_id";
    }
    private String visibleWhere() {
        return " WHERE c.tenant_id=? AND c.event_sink='" + (application.isMultiTenant() ? "VLS" : "LOCAL")
            + "' AND ((c.event_sink='VLS' AND e.is_deleted=0) OR (c.event_sink='LOCAL' AND l.deleted_at=0))";
    }
    private String statusExpression() { return "CASE WHEN c.event_sink='LOCAL' THEN IF(l.status=1,'completed','processing') ELSE e.event_status END"; }
    private String select() {
        return "SELECT c.*,CAST(c.event_id AS CHAR) AS eventId,COALESCE(e.event_type,l.item) event_type,"
            + "COALESCE(e.event_desc,JSON_UNQUOTE(JSON_EXTRACT(l.data,'$.describe'))) event_desc,"
            + statusExpression() + " event_status,COALESCE(e.report_device,l.device_id) report_device,"
            + "COALESCE(e.report_location,JSON_UNQUOTE(JSON_EXTRACT(l.data,'$.point.address'))) report_location,e.handle_result"
            + from();
    }
    private Map<String,Object> decorate(Map<String,Object> row, boolean detail) {
        row.put("detail", service.read(String.valueOf(row.remove("detail_json")))); row.remove("tenant_id"); row.remove("event_id");
        Map<String,String> urls = new LinkedHashMap<>();
        for (String role : detail ? Arrays.asList("original", "annotated", "video") : Collections.singletonList("annotated")) {
            Object id = row.get(role + "_media_id");
            if (id != null) { try { urls.put(role, media.getPrivateViewUrl(String.valueOf(id),300)); } catch (RuntimeException error) { urls.put(role, null); } }
        }
        row.put("media", urls); return row;
    }
    static JsonNode member(JsonNode array, String key, String value) {
        for (JsonNode item : array) if (value.equals(item.path(key).asText())) return item;
        throw new ServiceException("检测事件不属于本次冻结的摄像机和模型");
    }
    static String eventDeviceId(JsonNode camera) {
        // Native channel identities can exceed the legacy event table's 40-character key.
        // Keep the stable device reference here; retain the full channel identity in evidence metadata.
        String protocol=camera.path("protocol").asText(), row=camera.path("deviceRowId").asText();
        if (!"VLSTREAM".equals(protocol) && protocol.matches("ONVIF|RTSP|CUSTOM|ISUP|EHOME|GB28181|DAHUA") && row.matches("[0-9]{1,20}")) return protocol+":"+row;
        String id=camera.path("deviceId").asText();
        if (id.isEmpty() || id.codePointCount(0,id.length())>40) throw new ServiceException("摄像机事件关联编号无效");
        return id;
    }
}
