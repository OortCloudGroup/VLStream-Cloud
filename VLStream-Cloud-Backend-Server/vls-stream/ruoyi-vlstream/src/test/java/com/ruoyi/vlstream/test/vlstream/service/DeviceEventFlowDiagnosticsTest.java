package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class DeviceEventFlowDiagnosticsTest {
    @Test void distinguishesHeartbeatFaceStructAndIgnoredPlatformReplies() throws Exception {
        DeviceEventFlowDiagnostics counts = new DeviceEventFlowDiagnostics();
        VlsMqttInboundDispatcher dispatcher = new VlsMqttInboundDispatcher(Collections.emptyList());
        field(dispatcher, "diagnostics", counts);
        dispatcher.dispatch(VlsMqttProtocol.deviceBusTopic("fixture-device"), envelope("device", "state").toString());
        dispatcher.dispatch(VlsMqttProtocol.deviceBusTopic("fixture-device"), envelope("aiBiz", "faceEvent").toString());
        dispatcher.dispatch(VlsMqttProtocol.deviceBusTopic("fixture-device"), envelope("aiBiz", "struct").toString());
        dispatcher.dispatch(VlsMqttProtocol.deviceBusTopic("fixture-device"), envelope("aiBiz", "faceEvent")
            .set("msgDir", "platform2dev").toString());
        assertEquals(3, count(counts, "MQTT_DEVICE_ENVELOPE"));
        assertEquals(1, count(counts, "MQTT_DEVICE_STATE"));
        assertEquals(1, count(counts, "MQTT_AI_FACE"));
        assertEquals(1, count(counts, "MQTT_AI_STRUCT"));
        assertEquals(1, count(counts, "MQTT_IGNORED_ENVELOPE"));
        assertEquals(3, count(counts, "MQTT_UNSUPPORTED"));
        assertFalse(counts.snapshot().toString().contains("fixture-device"));
    }

    @Test void observesTheRealBusDispatchEntryWithoutPublishingOrConnecting() throws Exception {
        DeviceEventFlowDiagnostics counts = new DeviceEventFlowDiagnostics();
        VlsMqttBusService bus = new VlsMqttBusService();
        field(bus, "diagnostics", counts);
        field(bus, "inboundDispatcher", mock(VlsMqttInboundDispatcher.class));
        try {
            bus.handleIncomingMessage("fixture-topic", "fixture-payload");
            assertEquals(1, count(counts, "MQTT_RECEIVED"));
            assertFalse(counts.snapshot().toString().contains("fixture-payload"));
        } finally { bus.destroy(); }
    }

    @Test void businessRejectionRecordsOnlyAFixedStage() throws Exception {
        DeviceEventFlowDiagnostics counts = new DeviceEventFlowDiagnostics();
        DeviceEventMqttHandler handler = new DeviceEventMqttHandler();
        field(handler, "diagnostics", counts);
        VlsEventReportApplicationService application = mock(VlsEventReportApplicationService.class);
        when(application.isMultiTenant()).thenReturn(true);
        field(handler, "eventReportApplicationService", application);
        WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
        when(resolver.resolveEventDevice(anyString())).thenThrow(new ServiceException("fixture-sensitive-detail"));
        field(handler, "wvpDeviceResolver", resolver);
        handler.handle(envelope("aiBiz", "faceEvent"));
        assertEquals(1, count(counts, "EVENT_REJECT_DEVICE"));
        assertEquals(0, count(counts, "EVENT_BUSINESS_COMMITTED"));
        assertFalse(counts.snapshot().toString().contains("fixture-sensitive-detail"));
    }

    @Test void rollbackCannotBeCountedAsCommittedBusiness() {
        DeviceEventFlowDiagnostics counts = new DeviceEventFlowDiagnostics();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            counts.afterCommit(DeviceEventFlowDiagnostics.Counter.EVENT_BUSINESS_COMMITTED);
            assertEquals(0, count(counts, "EVENT_BUSINESS_COMMITTED"));
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertEquals(0, count(counts, "EVENT_BUSINESS_COMMITTED"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            counts.afterCommit(DeviceEventFlowDiagnostics.Counter.EVENT_BUSINESS_COMMITTED);
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
            assertEquals(1, count(counts, "EVENT_BUSINESS_COMMITTED"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private static JSONObject envelope(String main, String sub) {
        return new JSONObject().set("protocolVersion", "2.2").set("msgDir", "dev2platform")
            .set("deviceId", "fixture-device").set("messageId", "fixture-message")
            .set("mainBizType", main).set("subBizType", sub).set("payload", new JSONObject());
    }
    private static long count(DeviceEventFlowDiagnostics counts, String key) {
        Map<?, ?> all = (Map<?, ?>) counts.snapshot().get("counters");
        return ((Number) ((Map<?, ?>) all.get(key)).get("count")).longValue();
    }
    private static void field(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
