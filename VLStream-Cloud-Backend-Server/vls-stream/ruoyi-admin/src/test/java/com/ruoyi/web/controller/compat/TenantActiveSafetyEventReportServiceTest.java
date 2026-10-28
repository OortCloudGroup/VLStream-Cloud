package com.ruoyi.web.controller.compat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TenantActiveSafetyEventReportServiceTest {
    private final LocationTaskCompatService single = mock(LocationTaskCompatService.class);
    private final IVlsEventManagementService events = mock(IVlsEventManagementService.class);
    private final VlsEventManagementMapper eventMapper = mock(VlsEventManagementMapper.class);
    private final EventReportOutboxMapper outbox = mock(EventReportOutboxMapper.class);
    private final WvpVlStreamDeviceResolver devices = mock(WvpVlStreamDeviceResolver.class);
    private final DeviceMediaUploadService media = mock(DeviceMediaUploadService.class);
    private final LlmReviewTaskService reviews = mock(LlmReviewTaskService.class);
    private final VlsEventReportApplicationService application = new VlsEventReportApplicationService(
        events, eventMapper, outbox, mock(IFileUploadService.class));
    private AnnotationConfigApplicationContext context;
    private ActiveSafetyEventReportService sink;
    private DeviceEventMqttHandler handler;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(application, "tenantType", "multi");
        when(events.createEvent(any())).thenAnswer(call -> {
            EventManagement event = call.getArgument(0);
            assertEquals("tenant-a", TenantContextHolder.getTenantId());
            event.setId(99L); return true;
        });
        when(outbox.insert(any(EventReportOutbox.class))).thenReturn(1);
        DeviceInfo device = new DeviceInfo();
        device.setDeviceId("CAM-1"); device.setDeviceName("camera"); device.setTenantId("tenant-a");
        when(devices.resolve("CAM-1")).thenReturn(device);
        when(devices.resolveEventDevice("CAM-1")).thenReturn(device);
        DeviceMediaUpload image = new DeviceMediaUpload(); image.setContentType("image/jpeg");
        DeviceMediaUpload video = new DeviceMediaUpload(); video.setContentType("video/mp4");
        when(media.validateAndBind(eq("image-1"), anyString(), anyString(), anyString(), anyString())).thenReturn(image);
        when(media.validateAndBind(eq("video-1"), anyString(), anyString(), anyString(), anyString())).thenReturn(video);
        context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("locationTaskCompatService", single);
        context.getBeanFactory().registerSingleton("eventReportApplicationService", application);
        context.getBeanFactory().registerSingleton("wvpDeviceResolver", devices);
        context.getBeanFactory().registerSingleton("mediaUploadService", media);
        context.getBeanFactory().registerSingleton("deviceMediaProperties", new VlsDeviceMediaProperties());
        context.getBeanFactory().registerSingleton("llmReviewTaskService", reviews);
        context.register(TenantActiveSafetyEventReportService.class, DeviceEventMqttHandler.class);
        context.refresh();
        sink = context.getBean(ActiveSafetyEventReportService.class);
        handler = context.getBean(DeviceEventMqttHandler.class);
    }

    @AfterEach void cleanup() { if (context != null) context.close(); TenantContextHolder.clear(); }

    @Test void multiStructUsesExternalOutboxAndKeepsImageVideoAndLongDescription() {
        assertInstanceOf(TenantActiveSafetyEventReportService.class, sink);
        TenantContextHolder.setTenantId("previous");
        JSONObject reply = handler.handle(envelope("struct"));
        assertEquals("SUCCESS", reply.getByPath("payload.bizData.status"));
        assertEquals("previous", TenantContextHolder.getTenantId());
        ArgumentCaptor<EventReportOutbox> task = ArgumentCaptor.forClass(EventReportOutbox.class);
        verify(outbox).insert(task.capture());
        JSONObject payload = JSONUtil.parseObj(task.getValue().getPayloadJson());
        assertEquals("tenant-a", task.getValue().getTenantId());
        assertEquals("tenant-a", payload.getStr("device_tenant_id"));
        assertEquals("IEV1805", payload.getStr("item"));
        assertEquals("this description is longer than ten characters", payload.getStr("describe"));
        assertEquals("vls-media://image-1", payload.getJSONArray("pics").getStr(0));
        assertEquals("vls-media://video-1", payload.getJSONArray("video").getStr(0));
        verifyNoInteractions(single);
    }

    @Test void multiFaceUsesSameSinkAndSkipsVisualReview() {
        JSONObject reply = handler.handle(envelope("faceEvent"));
        assertEquals("SUCCESS", reply.getByPath("payload.bizData.status"));
        verify(outbox).insert(any(EventReportOutbox.class));
        verifyNoInteractions(single, reviews);
    }

    @Test void everyDeviceUsesMultiFlowWithItsOwnTrustedTenantRegardlessOfUiTenant() {
        DeviceInfo second = new DeviceInfo();
        second.setDeviceId("CAM-2"); second.setDeviceName("camera2"); second.setTenantId("tenant-b");
        when(devices.resolve("CAM-2")).thenReturn(second);
        when(devices.resolveEventDevice("CAM-2")).thenReturn(second);
        java.util.concurrent.atomic.AtomicLong id = new java.util.concurrent.atomic.AtomicLong(100);
        doAnswer(call -> {
            EventManagement event = call.getArgument(0);
            assertEquals(event.getTenantId(), TenantContextHolder.getTenantId());
            event.setId(id.incrementAndGet()); return true;
        }).when(events).createEvent(any());
        TenantContextHolder.setTenantId("synthetic-ui-tenant");
        for (String deviceId : new String[]{"CAM-1", "CAM-2"}) {
            for (String type : new String[]{"struct", "faceEvent"}) {
                JSONObject message = envelope(type);
                message.set("deviceId", deviceId);
                message.set("messageId", deviceId + "-" + type);
                message.getJSONObject("payload").set("eventId", deviceId + "-" + type);
                assertEquals("SUCCESS", handler.handle(message).getByPath("payload.bizData.status"));
                assertEquals("synthetic-ui-tenant", TenantContextHolder.getTenantId());
            }
        }
        ArgumentCaptor<EventReportOutbox> tasks = ArgumentCaptor.forClass(EventReportOutbox.class);
        verify(outbox, times(4)).insert(tasks.capture());
        for (EventReportOutbox task : tasks.getAllValues()) {
            JSONObject payload = JSONUtil.parseObj(task.getPayloadJson());
            String tenant = "CAM-1".equals(payload.getStr("device_id")) ? "tenant-a" : "tenant-b";
            assertEquals(tenant, task.getTenantId());
            assertEquals(tenant, payload.getStr("device_tenant_id"));
        }
        verifyNoInteractions(single);
    }

    @Test void singleFaceAndStructContinueToUseOnlyTheOriginalLocalSink() {
        ReflectionTestUtils.setField(application, "tenantType", "single");
        when(single.report(any())).thenReturn(ActiveSafetyEventReportResult.builder().activeSafetyEventId("single-event").build());
        for (String type : new String[]{"struct", "faceEvent"}) {
            assertEquals("事件已接收并入库", handler.handle(envelope(type)).getByPath("payload.msg"));
        }
        verify(single, times(2)).report(any());
        verifyNoInteractions(events, outbox);
    }

    @Test void enabledReviewDefersOutboxAndConfirmationUsesSameTenantSink() {
        when(reviews.enqueueIfRequired(any(), any(), any(), anyString(), anyString(), anyString(), any())).thenReturn(true);
        assertEquals("事件已接收，等待大模型复核", handler.handle(envelope("struct")).getByPath("payload.msg"));
        verifyNoInteractions(outbox, events, single);
        TenantContextHolder.setTenantId("tenant-a");
        ActiveSafetyEventReportResult result = sink.report(report());
        assertEquals("99", result.getActiveSafetyEventId());
        verify(outbox).insert(any(EventReportOutbox.class));
        verifyNoInteractions(single);
    }

    @Test void singleKeepsOriginalBusinessSink() {
        ReflectionTestUtils.setField(application, "tenantType", "single");
        ActiveSafetyEventReportResult expected = ActiveSafetyEventReportResult.builder().activeSafetyEventId("local-event").build();
        when(single.report(any())).thenReturn(expected);
        assertSame(expected, sink.report(report()));
        sink.findDuplicate("message-1", "CAM-1", "event-1");
        verify(single).findDuplicate("message-1", "CAM-1", "event-1");
        verifyNoInteractions(events, outbox, devices);
    }

    @Test void actualAutomaticReviewConfirmationUsesTheMultiSink() {
        ReviewFixture fixture = new ReviewFixture();
        when(fixture.vision.review(any(), anyString(), anyList())).thenReturn(fixture.decision("CONFIRMED"));
        TenantContextHolder.setTenantId("tenant-a");
        fixture.service.processClaimed(fixture.task, "worker-1");
        verify(outbox).insert(any(EventReportOutbox.class));
        verifyNoInteractions(single);
    }

    @Test void actualRejectedAndUncertainReviewsDoNotGenerateFormalEvents() {
        ReviewFixture fixture = new ReviewFixture();
        TenantContextHolder.setTenantId("tenant-a");
        for (String decision : new String[]{"REJECTED", "UNCERTAIN"}) {
            when(fixture.vision.review(any(), anyString(), anyList())).thenReturn(fixture.decision(decision));
            fixture.service.processClaimed(fixture.task, "worker-1");
        }
        verifyNoInteractions(single, events, outbox);
    }

    @Test void actualManualReviewConfirmationUsesTheMultiSinkWithoutCallingTheModel() {
        ReviewFixture fixture = new ReviewFixture();
        TenantContextHolder.setTenantId("tenant-a");
        assertEquals("99", fixture.service.manualDecision(7L, "CONFIRMED").getFormalEventId());
        verify(outbox).insert(any(EventReportOutbox.class));
        verifyNoInteractions(single, fixture.vision);
    }

    @Test void missingTenantAndDeviceTenantChangeCannotCreateEventOrOutbox() {
        assertThrows(ServiceException.class, () -> sink.report(report()));
        TenantContextHolder.setTenantId("tenant-b");
        assertThrows(ServiceException.class, () -> sink.report(report()));
        verifyNoInteractions(events, outbox, single);
    }

    @Test void duplicateNeverRevalidatesMediaOrCreatesAnotherOutbox() {
        EventManagement existing = new EventManagement(); existing.setId(99L); existing.setMediaId("image-1");
        when(eventMapper.selectOne(any())).thenReturn(existing);
        assertEquals("事件已处理，重复消息已忽略", handler.handle(envelope("struct")).getByPath("payload.msg"));
        verifyNoInteractions(media, events, outbox, single, reviews);
    }

    private ActiveSafetyEventReport report() {
        return ActiveSafetyEventReport.builder().sourceMessageId("message-1").deviceEventId("event-1")
            .deviceId("CAM-1").deviceName("camera").mediaId("image-1").eventType("IEV1805")
            .description("confirmed by review").eventTime(new Date()).mqttPayloadJson("{}")
            .imageMediaIds(Collections.singletonList("image-1")).videoMediaIds(Collections.singletonList("video-1")).build();
    }

    private JSONObject envelope(String type) {
        return JSONUtil.parseObj("{\"messageId\":\"message-1\",\"deviceId\":\"CAM-1\",\"subBizType\":\"" + type + "\","
            + "\"payload\":{\"eventId\":\"event-1\",\"eventType\":\"IEV1805\","
            + "\"eventDesc\":\"this description is longer than ten characters\",\"media\":["
            + "{\"mediaId\":\"image-1\",\"objectKey\":\"image.jpg\",\"sha256\":\"abc\"},"
            + "{\"mediaId\":\"video-1\",\"objectKey\":\"video.mp4\",\"sha256\":\"def\"}]}}");
    }

    private class ReviewFixture {
        final com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper tasks = mock(com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper.class);
        final OpenAiVisionClient vision = mock(OpenAiVisionClient.class);
        final com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask task = new com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask();
        final LlmReviewTaskService service;

        ReviewFixture() {
            com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper providers = mock(com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper.class);
            com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider provider = new com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider();
            provider.setId(22L); provider.setEnabled(true); provider.setApiKeyCiphertext("synthetic-cipher");
            when(providers.selectById(22L)).thenReturn(provider);
            task.setId(7L); task.setTenantId("tenant-a"); task.setProviderId(22L); task.setMediaId("image-1");
            task.setDeviceId("CAM-1"); task.setSourceMessageId("message-1"); task.setDeviceEventId("event-1");
            task.setAttemptCount(0); task.setReviewStatus("MANUAL_REVIEW");
            task.setConfigSnapshotJson("{\"decisionThreshold\":0.8,\"imageMode\":\"FULL\",\"maxRetries\":0}");
            task.setEventPayloadJson("{\"sourceMessageId\":\"message-1\",\"deviceEventId\":\"event-1\",\"deviceId\":\"CAM-1\","
                + "\"deviceName\":\"camera\",\"eventType\":\"IEV1805\",\"description\":\"reviewed event\",\"mediaId\":\"image-1\",\"eventTime\":1000,\"mqttPayload\":{}}");
            when(tasks.selectById(7L)).thenReturn(task);
            when(tasks.updateById(any())).thenReturn(1);
            when(tasks.finishAttempt(any(), anyString(), anyString(), anyString(), anyInt(), any(), nullable(String.class),
                nullable(java.math.BigDecimal.class), nullable(String.class), nullable(String.class), nullable(String.class),
                nullable(String.class), any())).thenReturn(1);
            when(media.readBoundImage(anyString(), anyInt())).thenReturn(new byte[]{1});
            service = new LlmReviewTaskService(new com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties(),
                mock(com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmLlmReviewConfigMapper.class), providers,
                mock(LlmSystemProviderService.class), tasks, media, vision, sink);
        }

        OpenAiVisionClient.VisionDecision decision(String type) {
            OpenAiVisionClient.VisionDecision decision = new OpenAiVisionClient.VisionDecision();
            decision.setDecision(type); decision.setConfidence(new java.math.BigDecimal("0.95"));
            decision.setReason("synthetic review"); return decision;
        }
    }
}
