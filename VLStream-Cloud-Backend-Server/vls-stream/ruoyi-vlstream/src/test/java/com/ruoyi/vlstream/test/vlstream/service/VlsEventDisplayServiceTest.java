package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsEventDisplayServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final EventReportOutboxMapper outbox = mock(EventReportOutboxMapper.class);
    private final DeviceMediaUploadService media = mock(DeviceMediaUploadService.class);
    private final VlsEventDisplayService display = new VlsEventDisplayService(json, outbox, media,
        new EventLocationService(new WvpVlStreamDeviceProperties(), json), mock(ExternalEventImageService.class));

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    /** Older camera position payloads and explicit saved addresses work without device or outbox lookup. */
    @Test
    void prefersAStoredAddressAndShowsCoordinatesWhenOnlyAPointWasReported() {
        TenantContextHolder.setTenantId("tenant-a");
        EventManagementVO camera = event(1, "tenant-a", null, null, null);
        camera.setReportLocation(" 历史地址 ");
        camera.setEventData("{\"point\":{\"address\":\"重复地址\",\"lng\":114.1,\"lat\":22.5}}");
        EventManagementVO coordinates = event(2, "tenant-a", null, null, null);
        coordinates.setReportLocation(" ");
        coordinates.setEventData("{\"point\":{\"lng\":0,\"lat\":0,\"coord_system_type\":1}}");
        display.enrich(Arrays.asList(camera, coordinates), null);

        assertEquals("历史地址", camera.getDisplayLocation().get("address"));
        assertEquals("EVENT", coordinates.getDisplayLocation().get("source"));
        assertEquals("COORDINATES", coordinates.getDisplayLocation().get("status"));
        assertEquals("0", coordinates.getDisplayLocation().get("longitude").toString());
        verifyNoInteractions(outbox);
    }

    /** Only a locally bound image for the owned event may produce a short-lived preview URL. */
    @Test
    void signsOwnedImagesIncludingLegacyAndCloudReferencesButSkipsVideosAndWrongEvents() {
        TenantContextHolder.setTenantId("tenant-a");
        EventManagementVO privateImage = event(1, "tenant-a", "vls-media://capture", "first-video", "message-1");
        EventManagementVO legacy = event(2, "tenant-a", null, "legacy", "message-2");
        EventManagementVO video = event(3, "tenant-a", null, "video", "message-3");
        EventManagementVO otherEvent = event(4, "tenant-a", "vls-media://capture", null, "message-4");
        EventManagementVO cloud = event(5, "tenant-a", "vls-media://cloud", null, "cloud-device:detection");
        cloud.setDeviceEventId("detection");
        EventManagementVO external = event(6, "tenant-a", null, "external", "message-6");
        EventManagementVO otherTenant = event(7, "tenant-b", "vls-media://foreign", null, "message-7");
        EventManagementVO broken = event(8, "tenant-a", "vls-media://broken", null, "message-8");

        DeviceMediaUpload capture = upload("capture", "image/jpeg", "message-1");
        DeviceMediaUpload legacyImage = upload("legacy", "image/png", "message-2");
        DeviceMediaUpload clip = upload("video", "video/mp4", "message-3");
        DeviceMediaUpload evidence = upload("cloud", "image/jpeg", "cloud-ai:detection");
        evidence.setObjectKey("cloud-ai/tenant/run/detection/annotated.jpg");
        evidence.setDeviceId("camera-source");
        DeviceMediaUpload unavailable = upload("broken", "image/jpeg", "message-8");
        Map<String, DeviceMediaUpload> uploads = new HashMap<>();
        for (DeviceMediaUpload upload : Arrays.asList(capture, legacyImage, clip, evidence, unavailable)) {
            uploads.put(upload.getMediaId(), upload);
        }
        when(media.findBoundUploads("tenant-a", new LinkedHashSet<>(Arrays.asList("capture", "legacy", "video", "cloud", "external", "broken"))))
            .thenReturn(uploads);
        when(media.signViewUrl(capture, 3600)).thenReturn("https://storage.invalid/capture.jpg");
        when(media.signViewUrl(legacyImage, 3600)).thenReturn("https://storage.invalid/legacy.png");
        when(media.signViewUrl(evidence, 3600)).thenReturn("https://storage.invalid/cloud.jpg");
        doThrow(new IllegalStateException()).when(media).signViewUrl(unavailable, 3600);

        display.enrich(Arrays.asList(privateImage, legacy, video, otherEvent, cloud, external, otherTenant, broken), null);

        assertEquals("https://storage.invalid/capture.jpg", privateImage.getReportImageUrl());
        assertEquals("https://storage.invalid/legacy.png", legacy.getReportImageUrl());
        assertEquals("https://storage.invalid/cloud.jpg", cloud.getReportImageUrl());
        assertEquals("vls-media://capture", privateImage.getReportImg());
        for (EventManagementVO missing : Arrays.asList(video, otherEvent, external, otherTenant, broken)) {
            assertNull(missing.getReportImageUrl());
        }
        assertNull(otherTenant.getDisplayLocation());
        verify(media, never()).signViewUrl(clip, 3600);
        verify(media).signViewUrl(capture, 3600);
    }

    /** A valid saved address keeps image-only fixtures independent from heartbeat state. */
    private static EventManagementVO event(long id, String tenant, String reference, String mediaId, String messageId) {
        EventManagementVO event = new EventManagementVO();
        event.setId(id);
        event.setTenantId(tenant);
        event.setReportImg(reference);
        event.setMediaId(mediaId);
        event.setMqttMessageId(messageId);
        event.setReportDevice("camera-1");
        event.setReportLocation("历史地址");
        return event;
    }

    /** Model locally bound media after tenant-scoped lookup. */
    private static DeviceMediaUpload upload(String id, String type, String binding) {
        DeviceMediaUpload upload = new DeviceMediaUpload();
        upload.setMediaId(id);
        upload.setContentType(type);
        upload.setBoundEventMessageId(binding);
        upload.setDeviceId("camera-1");
        return upload;
    }
}
