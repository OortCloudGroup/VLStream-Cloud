package com.ruoyi.vlstream.test.vlstream.cloudai;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.VlsEventReportApplicationService;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper;
import com.ruoyi.vlstream.test.vlstream.service.OpenAiVisionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudAiReviewPublisher {
    private final JdbcTemplate jdbc;
    private final VlsEventReportApplicationService application;
    private final CloudAiLocalEventSink local;
    private final LlmReviewTaskMapper tasks;

    @Transactional(rollbackFor = Exception.class)
    public void finishConfirmed(LlmReviewTask task, JSONObject event, String workerId, int attempt,
                                OpenAiVisionClient.VisionDecision decision, Date now) {
        // Manual corrections lock the task before the cloud event as well.
        LlmReviewTask current = tasks.findExistingForUpdate(task.getTenantId(), task.getDeviceId(), task.getDeviceEventId());
        if (current == null || !"PROCESSING".equals(current.getReviewStatus()) || !workerId.equals(current.getLockedBy())) return;
        String id = confirm(task, event);
        if (tasks.finishAttempt(task.getId(), task.getTenantId(), workerId, "CONFIRMED", attempt, now,
                decision.getDecision(), decision.getConfidence(), decision.getReason(), decision.getRawResponse(), null, id, now) != 1) {
            throw new ServiceException("复核任务状态已变化");
        }
    }

    static boolean pending(Map<String,Object> row) {
        Object flag = row.get("review_pending");
        return Boolean.TRUE.equals(flag) || "1".equals(String.valueOf(flag));
    }

    void create(EventManagement event, DeviceInfo device, String original) {
        if (application.isMultiTenant()) {
            application.persistCloudAiEvent(event, device);
        } else {
            local.create(device.getTenantId(), event.getId(), ActiveSafetyEventReport.builder()
                .sourceMessageId(event.getMqttMessageId()).deviceEventId(event.getDeviceEventId())
                .deviceId(device.getDeviceId()).deviceName(device.getDeviceName())
                .eventType(event.getEventType()).description(event.getEventDesc()).eventTime(event.getReportTime())
                .mediaId(event.getMediaId()).imageMediaIds(Arrays.asList(event.getMediaId(), original))
                .videoMediaIds(Collections.emptyList()).mqttPayloadJson(event.getEventData()).address(device.getAddress()).build());
        }
    }

    void complete(EventManagement event, DeviceInfo device, List<String> images, List<String> videos) {
        if (application.isMultiTenant()) application.publishCloudAiEvidence(event, device, images, videos);
        else local.completeEvidence(device.getTenantId(), event.getId(), images, videos);
    }

    // Lock the same row as evidence ingestion. Either arrival order delivers the video exactly once.
    @Transactional(rollbackFor = Exception.class)
    public String confirm(LlmReviewTask task, JSONObject snapshot) {
        Map<String,Object> row = jdbc.queryForMap("SELECT * FROM vls_cloud_ai_event WHERE id=? AND tenant_id=? AND scene_id=? FOR UPDATE",
            task.getDeviceEventId(), task.getTenantId(), snapshot.getStr("sceneId"));
        if (application.isMultiTenant() != "VLS".equals(row.get("event_sink"))) throw new ServiceException("场景事件运行模式不匹配");
        long id = ((Number)row.get("event_id")).longValue();
        if (!pending(row)) return String.valueOf(id);
        String image = (String)row.get("annotated_media_id");
        String original = (String)row.get("original_media_id");
        EventManagement event = new EventManagement();
        event.setId(id); event.setMqttMessageId(task.getSourceMessageId()); event.setDeviceEventId(task.getDeviceEventId());
        event.setMediaId(image); event.setReportImg(VlsEventReportApplicationService.mediaReference(image));
        event.setEventType(snapshot.getStr("eventType")); event.setEventDesc(snapshot.getStr("description"));
        event.setReportDevice(task.getDeviceId()); event.setReportLocation(snapshot.getStr("address"));
        event.setReportTime(new Date(snapshot.getLong("eventTime"))); event.setEventData(snapshot.getStr("cloudEventData"));
        DeviceInfo device = new DeviceInfo();
        device.setTenantId(task.getTenantId()); device.setDeviceId(task.getDeviceId());
        device.setDeviceName(snapshot.getStr("deviceName")); device.setAddress(snapshot.getStr("address"));
        create(event, device, original);
        if (!"PENDING".equals(row.get("media_state"))) {
            String video = (String)row.get("video_media_id");
            complete(event, device, Arrays.asList(image, original), video == null ? Collections.emptyList() : Collections.singletonList(video));
        }
        jdbc.update("UPDATE vls_cloud_ai_event SET review_pending=0 WHERE id=? AND tenant_id=?", task.getDeviceEventId(), task.getTenantId());
        return String.valueOf(id);
    }
}
