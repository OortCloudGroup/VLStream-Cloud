package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class DeviceModelServiceTest {
    final WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
    final VlsMqttBusService bus = mock(VlsMqttBusService.class);
    final DeviceModelReplyHandler replies = new DeviceModelReplyHandler();
    final DeviceModelService service = new DeviceModelService(resolver, bus, replies);

    private void reply(JSONObject data, int code) {
        doAnswer(call -> {
            JSONObject request = call.getArgument(1);
            assertEquals("vlstream/v2.2/dev/CAM-1/bus", call.getArgument(0));
            assertEquals("platform2dev", request.getStr("msgDir"));
            JSONObject envelope = new JSONObject().set("deviceId", "CAM-1")
                .set("subBizType", request.getStr("subBizType"))
                .set("payload", new JSONObject().set("sourceMsgId", request.getStr("messageId"))
                    .set("code", code).set("msg", "设备拒绝").set("bizData", data));
            replies.handle(envelope, envelope.toString());
            return null;
        }).when(bus).publish(anyString(), any());
    }

    @Test void queryReturnsHardwareInventoryIncludingEmpty() {
        reply(new JSONObject().set("models", new JSONArray().put(new JSONObject().set("modelId", "9007199254740993"))), 200);
        assertEquals("9007199254740993", service.query("CAM-1").getJSONObject(0).getStr("modelId"));
        reply(new JSONObject().set("models", new JSONArray()), 200);
        assertTrue(service.query("CAM-1").isEmpty());
        verify(resolver, times(2)).resolveOnline("CAM-1");
    }

    @Test void legacyModelCanBeViewedButHasNoInventedDeleteId() {
        reply(new JSONObject().set("algorithmId", "123").set("fileName", "hat.om").set("modelType", "om"), 200);
        JSONObject model = service.query("CAM-1").getJSONObject(0);
        assertEquals("hat.om", model.getStr("modelName"));
        assertNull(model.getStr("modelId"));
    }

    @Test void malformedOrRejectedQueryDoesNotBecomeEmptySuccess() {
        reply(new JSONObject(), 200);
        assertThrows(ServiceException.class, () -> service.query("CAM-1"));
        reply(new JSONObject().set("models", new JSONArray()), 500);
        assertThrows(ServiceException.class, () -> service.query("CAM-1"));
    }

    @Test void rejectsNumericAndDuplicateModelIdentifiers() {
        reply(new JSONObject().set("models", new JSONArray().put(new JSONObject().set("modelId", 123))), 200);
        assertThrows(ServiceException.class, () -> service.query("CAM-1"));
        reply(new JSONObject().set("models", new JSONArray().put(new JSONObject().set("modelId", "same"))
            .put(new JSONObject().set("modelId", "same"))), 200);
        assertThrows(ServiceException.class, () -> service.query("CAM-1"));
    }

    @Test void deletionRequiresMatchingModelAndTerminalSuccess() {
        reply(new JSONObject().set("modelId", "m1").set("status", "SUCCESS"), 200);
        assertDoesNotThrow(() -> service.delete("CAM-1", "m1"));
        assertThrows(ServiceException.class, () -> service.delete("CAM-1", "m2"));
        reply(new JSONObject().set("modelId", "m1").set("status", "FAILED"), 200);
        assertThrows(ServiceException.class, () -> service.delete("CAM-1", "m1"));
    }

    @Test void offlineDeviceAndBlankModelNeverPublish() {
        assertThrows(ServiceException.class, () -> service.delete("CAM-1", " "));
        when(resolver.resolveOnline("CAM-1")).thenThrow(new ServiceException("设备离线"));
        assertThrows(ServiceException.class, () -> service.query("CAM-1"));
        verifyNoInteractions(bus);
    }

    @Test void timeoutAndPublishFailureCleanUpPendingRequest() throws Exception {
        DeviceModelReplyHandler registry = mock(DeviceModelReplyHandler.class);
        CompletableFuture<JSONObject> future = mock(CompletableFuture.class);
        when(registry.register(anyString(), anyString(), anyString())).thenReturn(future);
        when(future.get(15, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        DeviceModelService target = new DeviceModelService(resolver, bus, registry);
        assertTrue(assertThrows(ServiceException.class, () -> target.query("CAM-1")).getMessage().contains("超时"));
        verify(registry).remove(anyString());
        doThrow(new IllegalStateException("disconnected")).when(bus).publish(anyString(), any());
        assertThrows(ServiceException.class, () -> target.delete("CAM-1", "m1"));
        verify(registry, times(2)).remove(anyString());
    }

    @Test void ignoresWrongDeviceWrongOperationAndLateReplies() {
        CompletableFuture<JSONObject> future = replies.register("request", "CAM-1", "modelQuery");
        JSONObject envelope = new JSONObject().set("deviceId", "CAM-2").set("subBizType", "modelQuery")
            .set("payload", new JSONObject().set("sourceMsgId", "request"));
        replies.handle(envelope, "");
        assertFalse(future.isDone());
        envelope.set("deviceId", "CAM-1").set("subBizType", "modelDelete");
        replies.handle(envelope, "");
        assertFalse(future.isDone());
        replies.remove("request");
        envelope.set("subBizType", "modelQuery");
        replies.handle(envelope, "");
        assertFalse(future.isDone());
    }
}
