/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ruoyi.vlstream.test.vlstream.service.VlsEventReportApplicationService.MEDIA_REFERENCE_PREFIX;

/** Builds the multi-tenant event page and detail display fields without changing the stored report. */
@Service
@RequiredArgsConstructor
@Slf4j
public class VlsEventDisplayService {
    private static final int IMAGE_PREVIEW_SECONDS = 3600;
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<Map<String, Object>>() {};

    private final ObjectMapper json;
    private final EventReportOutboxMapper outbox;
    private final DeviceMediaUploadService media;
    private final EventLocationService locations;
    private final ExternalEventImageService externalImages;

    /** Enrich only already-authorized events of the current session tenant. */
    public void enrich(List<EventManagementVO> events, String verifiedToken) {
        String tenantId = TenantContextHolder.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            return;
        }
        List<EventManagementVO> owned = events.stream()
            .filter(event -> tenantId.equals(event.getTenantId()))
            .collect(Collectors.toList());
        if (owned.isEmpty()) {
            return;
        }
        addImages(owned, tenantId);
        externalImages.enrich(owned, tenantId);
        addLocations(owned, tenantId, verifiedToken);
    }

    /** Prefer the saved event position, then the original report snapshot, then the authorized heartbeat reference. */
    private void addLocations(List<EventManagementVO> events, String tenantId, String verifiedToken) {
        Map<Long, Map<String, Object>> displayRows = new LinkedHashMap<>();
        List<Long> missingPoints = new ArrayList<>();
        for (EventManagementVO event : events) {
            Map<String, Object> point = new LinkedHashMap<>(readPoint(event.getEventData()));
            if (StringUtils.isNotBlank(event.getReportLocation())) {
                point.put("address", event.getReportLocation());
            }
            if (EventLocationService.text(point.get("address")).isEmpty() && EventLocationService.coordinates(point) == null) {
                missingPoints.add(event.getId());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("point", point);
            row.put("tenant_id", tenantId);
            row.put("device_id", event.getReportDevice());
            row.put("created_at", event.getReportTime());
            displayRows.put(event.getId(), row);
        }
        if (!missingPoints.isEmpty()) {
            for (EventReportOutbox snapshot : outbox.selectList(new LambdaQueryWrapper<EventReportOutbox>()
                .select(EventReportOutbox::getEventId, EventReportOutbox::getPayloadJson)
                .eq(EventReportOutbox::getTenantId, tenantId)
                .in(EventReportOutbox::getEventId, missingPoints))) {
                displayRows.get(snapshot.getEventId()).put("point", readPoint(snapshot.getPayloadJson()));
            }
        }
        locations.enrich(new ArrayList<>(displayRows.values()), tenantId, verifiedToken);
        for (EventManagementVO event : events) {
            event.setDisplayLocation(EventLocationService.map(displayRows.get(event.getId()).get("display_location")));
        }
    }

    /** Old or non-location event payloads simply have no usable point. */
    private Map<String, Object> readPoint(String payload) {
        if (StringUtils.isNotBlank(payload)) {
            try {
                Map<String, Object> eventData = json.readValue(payload, JSON_OBJECT);
                return eventData == null ? Collections.emptyMap() : EventLocationService.map(eventData.get("point"));
            } catch (JsonProcessingException ignored) {
                // Some legacy producers stored plain text or arrays as event data.
            }
        }
        return Collections.emptyMap();
    }

    /** Resolve private image references in one batch, including legacy rows that only store a media ID. */
    private void addImages(List<EventManagementVO> events, String tenantId) {
        Map<Long, String> mediaIds = new LinkedHashMap<>();
        for (EventManagementVO event : events) {
            String reference = StringUtils.trimToEmpty(event.getReportImg());
            String mediaId = reference.startsWith(MEDIA_REFERENCE_PREFIX)
                ? reference.substring(MEDIA_REFERENCE_PREFIX.length())
                : (reference.isEmpty() ? event.getMediaId() : null);
            if (StringUtils.isNotBlank(mediaId)) {
                mediaIds.put(event.getId(), mediaId);
            }
        }
        Map<String, DeviceMediaUpload> uploads = media.findBoundUploads(tenantId, new LinkedHashSet<>(mediaIds.values()));
        for (EventManagementVO event : events) {
            DeviceMediaUpload upload = uploads.get(mediaIds.get(event.getId()));
            if (upload == null || !DeviceMediaUploadService.isSupportedImageType(upload.getContentType())
                || !isBoundToEvent(event, upload)) {
                continue;
            }
            try {
                event.setReportImageUrl(media.signViewUrl(upload, IMAGE_PREVIEW_SECONDS));
            } catch (RuntimeException exception) {
                log.warn("Could not sign preview for event {} image {}", event.getId(), upload.getMediaId());
            }
        }
    }

    /** Match the recorded MQTT binding or the deterministic binding of verified cloud evidence. */
    private boolean isBoundToEvent(EventManagement event, DeviceMediaUpload upload) {
        String binding = upload.getBoundEventMessageId();
        if (StringUtils.isNotBlank(binding) && binding.equals(event.getMqttMessageId())
            && StringUtils.equals(upload.getDeviceId(), event.getReportDevice())) {
            return true;
        }
        String detectionId = event.getDeviceEventId();
        if (StringUtils.isBlank(detectionId) || !StringUtils.startsWith(upload.getObjectKey(), "cloud-ai/")) {
            return false;
        }
        String evidenceBinding = "cloud-ai:" + detectionId;
        return evidenceBinding.equals(binding) && (evidenceBinding.equals(event.getMqttMessageId())
            || ("cloud-device:" + detectionId).equals(event.getMqttMessageId()));
    }
}
