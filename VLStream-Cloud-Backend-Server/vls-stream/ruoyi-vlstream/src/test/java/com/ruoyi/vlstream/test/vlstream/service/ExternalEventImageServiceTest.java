package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import com.ruoyi.vlstream.test.vlstream.service.ExternalEventImageService.ImageReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("dev")
class ExternalEventImageServiceTest {
    private static final String READER_TENANT = "tenant-a";
    private static final String TOKEN = "test-only-event-image-reader-credential";
    private static final String HOST = "https://media.example.invalid:2443";
    private static final String SOURCE = HOST + "/bus/vls-server";
    private static final String MEDIA = "12345678-abcd-4edf-9123-123456789abc";
    private static final String DEVICE = "CAM-1";
    private static final String MESSAGE = "event-source-1";
    private static final String DIGEST = String.join("", Collections.nCopies(64, "a"));
    private static final String KEY = "events/" + DEVICE + "/2026/11/26/" + MEDIA + ".jpg";
    private static final String URL = HOST + "/vlstream/" + KEY + "?X-Amz-Signature=synthetic";

    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final ObjectMapper json = new ObjectMapper();
    private final DeviceMediaUploadService media = mock(DeviceMediaUploadService.class);
    private final EventReportOutboxMapper outbox = mock(EventReportOutboxMapper.class);
    private final ExternalEventImageService service = new ExternalEventImageService(media, outbox, json, http);

    @BeforeAll
    static void initializeOutboxTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "event-image-test"),
            EventReportOutbox.class);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    /** Neither a browser session nor a mismatched tenant can use the source signing capability. */
    @Test
    void sourceRejectsMissingCredentialsForeignReadersAndMultiModeWithoutLoadingMedia() {
        configure("single");
        TenantContextHolder.setTenantId("000000");
        List<ImageReference> input = Collections.singletonList(reference(MESSAGE, DIGEST));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.signBoundImages(null, READER_TENANT, input)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.signBoundImages(TOKEN, "tenant-b", input)).getStatus());
        configure("multi");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.signBoundImages(TOKEN, READER_TENANT, input)).getStatus());
        verifyNoInteractions(media);
    }

    /** The exact source binding signs only an image, and mismatches stay null at their own request index. */
    @Test
    void sourceSignsOnlyTheExactBoundDeviceMessageKeyAndDigest() {
        configure("single");
        TenantContextHolder.setTenantId("000000");
        DeviceMediaUpload upload = new DeviceMediaUpload();
        upload.setMediaId(MEDIA);
        upload.setDeviceId(DEVICE);
        upload.setBoundEventMessageId(MESSAGE);
        upload.setObjectKey(KEY);
        upload.setSha256(DIGEST);
        upload.setContentType("image/jpeg");
        ImageReference valid = reference(MESSAGE, DIGEST);
        ImageReference invalidKey = new ImageReference(MEDIA, DEVICE, MESSAGE, "../" + KEY, DIGEST);
        ImageReference differentDevice = new ImageReference(MEDIA, "CAM-2", MESSAGE, KEY.replace(DEVICE, "CAM-2"), DIGEST);
        when(media.findBoundUploads("000000", Collections.singleton(MEDIA))).thenReturn(Collections.singletonMap(MEDIA, upload));
        when(media.signViewUrl(upload, 300)).thenReturn(URL);

        String[] signed = service.signBoundImages(TOKEN, READER_TENANT, Arrays.asList(
            reference("wrong-message", DIGEST), invalidKey, differentDevice, valid, reference(MESSAGE, DIGEST.replace('a', 'b'))));
        assertArrayEquals(new String[]{null, null, null, URL, null}, signed);
        verify(media).signViewUrl(upload, 300);
        upload.setContentType("video/mp4");
        assertArrayEquals(new String[]{null}, service.signBoundImages(TOKEN, READER_TENANT, Collections.singletonList(valid)));
    }

    /** A multi-tenant read uses one source call and never sends local session credentials or foreign rows. */
    @Test
    void consumerRequestsOnlyTheOwnedMqttImageAndKeepsAnExistingPreview() throws Exception {
        configure("multi");
        EventManagementVO owned = event(READER_TENANT);
        EventManagementVO foreign = event("tenant-b");
        EventManagementVO signedLocally = event(READER_TENANT);
        signedLocally.setId(2L);
        signedLocally.setReportImageUrl("https://local.example.invalid/private.jpg");
        EventManagementVO manual = event(READER_TENANT);
        manual.setId(3L);
        when(outbox.selectList(any())).thenReturn(Collections.singletonList(original(MESSAGE)));
        server.expect(requestTo(SOURCE + ExternalEventImageService.PATH))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(ExternalEventImageService.TOKEN_HEADER, TOKEN))
            .andExpect(header(ExternalEventImageService.TENANT_HEADER, READER_TENANT))
            .andExpect(request -> assertNull(request.getHeaders().getFirst("Authorization")))
            .andExpect(content().json(json.writeValueAsString(Collections.singletonList(reference(MESSAGE, DIGEST))), true))
            .andRespond(withSuccess(json.writeValueAsString(new String[]{URL}), MediaType.APPLICATION_JSON));

        service.enrich(Arrays.asList(owned, foreign, signedLocally, manual), READER_TENANT);
        assertEquals(URL, owned.getReportImageUrl());
        assertEquals(URL, json.valueToTree(owned).get("reportImg").asText());
        assertNull(owned.getReportImg(), "The source preview must not replace the stored reference");
        assertNull(foreign.getReportImageUrl());
        assertNull(manual.getReportImageUrl());
        assertEquals("https://local.example.invalid/private.jpg", signedLocally.getReportImageUrl());
        server.verify();
    }

    /** Source errors and returned URLs outside the configured origin must leave the event readable. */
    @Test
    void consumerIgnoresAnUnexpectedPreviewOriginAndHandlesUnavailability() throws Exception {
        configure("multi");
        EventManagementVO owned = event(READER_TENANT);
        when(outbox.selectList(any())).thenReturn(Collections.singletonList(original(MESSAGE)));
        server.expect(requestTo(SOURCE + ExternalEventImageService.PATH))
            .andRespond(withSuccess(json.writeValueAsString(new String[]{URL.replace(HOST, "https://wrong.invalid")}),
                MediaType.APPLICATION_JSON));
        server.expect(requestTo(SOURCE + ExternalEventImageService.PATH)).andRespond(withServerError());
        service.enrich(Collections.singletonList(owned), READER_TENANT);
        assertNull(owned.getReportImageUrl());
        service.enrich(Collections.singletonList(owned), READER_TENANT);
        assertNull(owned.getReportImageUrl());
        server.verify();
    }

    /** An event row edited to refer to a different MQTT message must not trigger a source lookup. */
    @Test
    void consumerRejectsAnEventThatNoLongerMatchesItsIngestSnapshot() throws Exception {
        configure("multi");
        EventManagementVO changed = event(READER_TENANT);
        when(outbox.selectList(any())).thenReturn(Collections.singletonList(original("original-message")));
        service.enrich(Collections.singletonList(changed), READER_TENANT);
        assertNull(changed.getReportImageUrl());
        server.verify();
    }

    /** Point the connector at a single synthetic authorized source. */
    private void configure(String mode) {
        ReflectionTestUtils.setField(service, "tenantMode", mode);
        ReflectionTestUtils.setField(service, "readerTenant", READER_TENANT);
        ReflectionTestUtils.setField(service, "accessToken", TOKEN);
        ReflectionTestUtils.setField(service, "sourceApiBase", SOURCE);
    }

    /** Build the same raw payload shape persisted by the device MQTT handler. */
    private EventManagementVO event(String tenant) throws Exception {
        EventManagementVO event = new EventManagementVO();
        event.setId(1L);
        event.setTenantId(tenant);
        event.setReportDevice(DEVICE);
        event.setMqttMessageId(MESSAGE);
        Map<String, String> capture = new HashMap<>();
        capture.put("mediaId", MEDIA);
        capture.put("objectKey", KEY);
        capture.put("sha256", DIGEST);
        event.setEventData(json.writeValueAsString(Collections.singletonMap("media", Collections.singletonList(capture))));
        return event;
    }

    /** Build the immutable outbox payload created by the device event application. */
    private EventReportOutbox original(String message) throws Exception {
        EventReportOutbox row = new EventReportOutbox();
        row.setEventId(1L);
        row.setTenantId(READER_TENANT);
        row.setPayloadJson(json.createObjectNode().put("source_event_id", 1L).put("source_message_id", message)
            .put("device_id", DEVICE).put("device_tenant_id", READER_TENANT).toString());
        return row;
    }

    /** Build the source's expected reference for a synthetic uploaded photo. */
    private ImageReference reference(String message, String digest) {
        return new ImageReference(MEDIA, DEVICE, message, KEY, digest);
    }
}
