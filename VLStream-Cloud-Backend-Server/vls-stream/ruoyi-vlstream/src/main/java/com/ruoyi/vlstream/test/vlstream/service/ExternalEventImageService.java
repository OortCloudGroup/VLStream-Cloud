/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** An explicitly configured single-tenant source may sign images already bound to the same MQTT event. */
@Service
@Slf4j
public class ExternalEventImageService {
    public static final String PATH = "/vlsDeviceMedia/internal/event-image-urls";
    public static final String TOKEN_HEADER = "X-VLS-Event-Image-Token";
    public static final String TENANT_HEADER = "X-VLS-Event-Image-Tenant";
    public static final int MAX_REFERENCES = 200;

    private static final int PREVIEW_SECONDS = 300;
    private static final Pattern MEDIA_ID = Pattern.compile("[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}");
    private static final Pattern DEVICE_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    private final DeviceMediaUploadService media;
    private final EventReportOutboxMapper outbox;
    private final ObjectMapper json;
    private final RestTemplate http;

    @Value("${token.tenant-type:single}")
    private String tenantMode;
    @Value("${vlstream.device-media.external-views.tenant-id:${VLSTREAM_EXTERNAL_EVENT_IMAGE_TENANT_ID:}}")
    private String readerTenant;
    @Value("${vlstream.device-media.external-views.access-token:${VLSTREAM_EXTERNAL_EVENT_IMAGE_ACCESS_TOKEN:}}")
    private String accessToken;
    @Value("${vlstream.device-media.external-views.source-api-base:${VLSTREAM_EXTERNAL_EVENT_IMAGE_SOURCE_API_BASE:}}")
    private String sourceApiBase;

    @Autowired
    public ExternalEventImageService(DeviceMediaUploadService media, EventReportOutboxMapper outbox, ObjectMapper json) {
        this(media, outbox, json, sourceClient());
    }

    ExternalEventImageService(DeviceMediaUploadService media, EventReportOutboxMapper outbox, ObjectMapper json, RestTemplate http) {
        this.media = media;
        this.outbox = outbox;
        this.json = json;
        this.http = http;
    }

    /** Authorize the configured reader; the source tenant only comes from this instance's trusted context. */
    private boolean permits(String token, String requestedTenant) {
        return "single".equalsIgnoreCase(tenantMode) && configuredReader(requestedTenant)
            && StringUtils.isNotBlank(TenantContextHolder.getTenantId()) && token != null
            && MessageDigest.isEqual(accessToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    /** Return one result per input, so a valid binding cannot satisfy a different event in the same batch. */
    public String[] signBoundImages(String token, String requestedTenant, List<ImageReference> references) {
        if (!permits(token, requestedTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (references == null || references.size() > MAX_REFERENCES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event image batch");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (ImageReference reference : references) {
            if (reference != null && reference.valid()) ids.add(reference.getMediaId());
        }
        Map<String, DeviceMediaUpload> uploads = media.findBoundUploads(TenantContextHolder.getTenantId(), ids);
        String[] urls = new String[references.size()];
        for (int i = 0; i < references.size(); i++) {
            ImageReference reference = references.get(i);
            if (reference == null || !reference.valid()) continue;
            DeviceMediaUpload upload = uploads.get(reference.getMediaId());
            if (upload == null || !reference.matches(upload)) continue;
            try {
                urls[i] = media.signViewUrl(upload, PREVIEW_SECONDS);
            } catch (RuntimeException exception) {
                log.warn("Could not sign source event image {}", reference.getMediaId());
            }
        }
        return urls;
    }

    /** Fill missing images on authorized multi-tenant events using the configured source only. */
    public void enrich(List<EventManagementVO> events, String tenantId) {
        if (!"multi".equalsIgnoreCase(tenantMode) || !configuredReader(tenantId)) return;
        URI endpoint = sourceEndpoint();
        if (endpoint == null) return;

        Map<Long, EventManagementVO> candidates = new HashMap<>();
        for (EventManagementVO event : events) {
            String existing = StringUtils.trimToEmpty(event.getReportImg());
            if (!tenantId.equals(event.getTenantId()) || StringUtils.isNotBlank(event.getReportImageUrl())
                || (!existing.isEmpty() && !existing.startsWith(VlsEventReportApplicationService.MEDIA_REFERENCE_PREFIX))
                || StringUtils.isBlank(event.getMqttMessageId()) || event.getId() == null) continue;
            candidates.put(event.getId(), event);
        }
        if (candidates.isEmpty()) return;

        List<ImageReference> references = new ArrayList<>();
        List<EventManagementVO> owners = new ArrayList<>();
        List<EventReportOutbox> originals = outbox.selectList(new LambdaQueryWrapper<EventReportOutbox>()
            .select(EventReportOutbox::getEventId, EventReportOutbox::getTenantId, EventReportOutbox::getPayloadJson)
            .eq(EventReportOutbox::getTenantId, tenantId).in(EventReportOutbox::getEventId, candidates.keySet()));
        for (EventReportOutbox original : originals) {
            EventManagementVO event = candidates.get(original.getEventId());
            if (event == null || !tenantId.equals(original.getTenantId()) || !matchesOriginal(event, original)) continue;
            for (ImageReference reference : reportedImages(event)) {
                references.add(reference);
                owners.add(event);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TOKEN_HEADER, accessToken);
        headers.set(TENANT_HEADER, tenantId);
        for (int from = 0; from < references.size(); from += MAX_REFERENCES) {
            List<ImageReference> batch = references.subList(from, Math.min(references.size(), from + MAX_REFERENCES));
            try {
                String[] urls = http.postForObject(endpoint, new HttpEntity<>(batch, headers), String[].class);
                if (urls == null || urls.length != batch.size()) return;
                for (int index = 0; index < urls.length; index++) {
                    EventManagementVO owner = owners.get(from + index);
                    if (owner.getReportImageUrl() == null && validPreview(endpoint, batch.get(index), urls[index])) {
                        owner.setReportImageUrl(urls[index]);
                    }
                }
            } catch (RestClientException exception) {
                log.debug("Source event image lookup unavailable ({})", exception.getClass().getSimpleName());
                return;
            }
        }
    }

    /** Only the original outbox snapshot proves this row was created from the trusted device report path. */
    private boolean matchesOriginal(EventManagementVO event, EventReportOutbox original) {
        if (StringUtils.isBlank(original.getPayloadJson())) return false;
        try {
            JsonNode snapshot = json.readTree(original.getPayloadJson());
            return String.valueOf(event.getId()).equals(snapshot.path("source_event_id").asText())
                && event.getMqttMessageId().equals(snapshot.path("source_message_id").asText())
                && StringUtils.isNotBlank(event.getReportDevice())
                && event.getReportDevice().equals(snapshot.path("device_id").asText())
                && event.getTenantId().equals(snapshot.path("device_tenant_id").asText());
        } catch (JsonProcessingException ignored) {
            return false;
        }
    }

    /** Never request a media ID or URL supplied by the current browser. */
    private List<ImageReference> reportedImages(EventManagementVO event) {
        if (StringUtils.isBlank(event.getEventData())) return Collections.emptyList();
        try {
            JsonNode payload = json.readTree(event.getEventData());
            JsonNode mediaList = payload.path("media");
            List<ImageReference> result = new ArrayList<>(2);
            if (mediaList.isArray() && !mediaList.isEmpty()) {
                if (mediaList.size() > 2) return result;
                for (JsonNode item : mediaList) addReference(result, event, item);
            } else {
                addReference(result, event, payload);
            }
            return result;
        } catch (JsonProcessingException ignored) {
            return Collections.emptyList();
        }
    }

    /** Build a reference from a persisted MQTT media entry; verify type at the source. */
    private void addReference(List<ImageReference> result, EventManagementVO event, JsonNode item) {
        ImageReference reference = new ImageReference(item.path("mediaId").asText(), event.getReportDevice(),
            event.getMqttMessageId(), item.path("objectKey").asText(), item.path("sha256").asText());
        if (reference.valid()) result.add(reference);
    }

    /** Require an explicit tenant mapping and a dedicated token long enough to be a service secret. */
    private boolean configuredReader(String tenant) {
        return StringUtils.isNotBlank(readerTenant) && readerTenant.equals(tenant)
            && accessToken != null && accessToken.length() >= 32 && StringUtils.isNotBlank(accessToken);
    }

    /** Use one fixed HTTPS API origin and never follow redirects or request a path from a reported event. */
    private URI sourceEndpoint() {
        if (StringUtils.isBlank(sourceApiBase)) return null;
        try {
            URI base = URI.create(StringUtils.removeEnd(sourceApiBase.trim(), "/"));
            if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null
                || base.getQuery() != null || base.getFragment() != null) return null;
            return URI.create(base + PATH);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Display only a signed HTTPS object URL from the approved source gateway and expected object key. */
    private boolean validPreview(URI endpoint, ImageReference reference, String value) {
        if (StringUtils.isBlank(value)) return false;
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && endpoint.getHost().equalsIgnoreCase(uri.getHost())
                && effectivePort(uri) == effectivePort(endpoint) && uri.getUserInfo() == null && uri.getFragment() == null
                && uri.getPath().endsWith("/" + reference.getObjectKey())
                && uri.getRawQuery() != null && uri.getRawQuery().contains("X-Amz-Signature=");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /** Normalize the implicit HTTPS port when comparing origins. */
    private int effectivePort(URI uri) {
        return uri.getPort() < 0 ? 443 : uri.getPort();
    }

    /** Bound source outages and prevent redirects from forwarding the service credential. */
    private static RestTemplate sourceClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(2000);
        return new RestTemplate(factory);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageReference {
        private String mediaId;
        private String deviceId;
        private String messageId;
        private String objectKey;
        private String sha256;

        /** Restrict the protocol to canonical device-upload object keys, with the exact original identity and digest. */
        boolean valid() {
            return mediaId != null && MEDIA_ID.matcher(mediaId).matches()
                && deviceId != null && DEVICE_ID.matcher(deviceId).matches()
                && StringUtils.isNotBlank(messageId) && messageId.length() <= 128
                && sha256 != null && SHA256.matcher(sha256).matches()
                && objectKey != null && objectKey.matches("events/" + Pattern.quote(deviceId)
                    + "/\\d{4}/\\d{2}/\\d{2}/" + mediaId + "\\.(jpg|png|webp)");
        }

        /** Exact device, MQTT message, object and digest binding in the source upload registry. */
        boolean matches(DeviceMediaUpload upload) {
            return mediaId.equals(upload.getMediaId()) && deviceId.equals(upload.getDeviceId())
                && messageId.equals(upload.getBoundEventMessageId()) && objectKey.equals(upload.getObjectKey())
                && sha256.equalsIgnoreCase(upload.getSha256())
                && DeviceMediaUploadService.isSupportedImageType(upload.getContentType());
        }
    }
}
