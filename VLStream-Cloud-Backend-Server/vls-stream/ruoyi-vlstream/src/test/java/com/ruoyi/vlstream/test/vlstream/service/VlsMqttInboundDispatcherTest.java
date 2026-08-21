package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsMqttInboundDispatcherTest {

	@Test
	void scheduledMaintenanceIsTheOnlyReconnectOwner() throws Exception {
		VlsMqttBusService busService = new VlsMqttBusService();
		setField(busService, "mqttProperties", new VlsMqttProperties());

		assertFalse(busService.connectOptions().isAutomaticReconnect());
		busService.destroy();
	}

	@Test
	void routesDeviceStateToRegisteredHandler() {
		MqttDeviceStateService stateService = mock(MqttDeviceStateService.class);
		JSONObject acknowledgement = new JSONObject();
		acknowledgement.put("messageId", "ack-state-1");
		when(stateService.handle(any())).thenReturn(acknowledgement);
		VlsMqttInboundDispatcher dispatcher = dispatcher(
			new DeviceStateMqttMessageHandler(stateService));
		String payload = envelope("deviceBiz", "state", "{}", "dev2platform");

		JSONObject result = dispatcher.dispatch("vlstream/v2.2/dev/CAM-1/bus", payload);

		verify(stateService).handle(any());
		org.junit.jupiter.api.Assertions.assertSame(acknowledgement, result);
	}

	@Test
	void routesRecognitionEventToRegisteredHandler() {
		DeviceEventMqttHandler eventService = mock(DeviceEventMqttHandler.class);
		JSONObject acknowledgement = new JSONObject();
		acknowledgement.put("messageId", "ack-event-1");
		when(eventService.handle(any())).thenReturn(acknowledgement);
		VlsMqttInboundDispatcher dispatcher = dispatcher(
			new DeviceEventMqttMessageHandler(eventService));

		JSONObject result = dispatcher.dispatch("vlstream/v2.2/dev/CAM-1/bus",
			envelope("aiBiz", "struct", "{}", "dev2platform"));

		verify(eventService).handle(any());
		org.junit.jupiter.api.Assertions.assertSame(acknowledgement, result);
	}

	@Test
	void appliesModelDeployReplyWithoutPublishingAnotherReply() {
		ModelDispatchTaskService taskService = mock(ModelDispatchTaskService.class);
		ModelDeployReplyMqttMessageHandler handler =
			new ModelDeployReplyMqttMessageHandler(taskService);
		VlsMqttInboundDispatcher dispatcher = dispatcher(handler);
		String replyPayload = "{\"sourceMsgId\":\"mqtt-1\",\"code\":200,"
			+ "\"msg\":\"deployed\",\"errCode\":0,\"errDetail\":\"\","
			+ "\"bizData\":{\"requestId\":\"request-1\",\"status\":\"SUCCESS\","
			+ "\"fileSha256\":\"abc\"}}";
		String reply = envelope("aiBiz", "modelDeploy", replyPayload, "dev2platform");
		when(taskService.applyHardwareReply(
			"mqtt-1", "request-1", "CAM-1", "SUCCESS", "abc", "deployed", reply))
			.thenReturn(true);

		JSONObject result = dispatcher.dispatch("vlstream/v2.2/dev/CAM-1/bus", reply);

		verify(taskService).applyHardwareReply(
			eq("mqtt-1"), eq("request-1"), eq("CAM-1"), eq("SUCCESS"),
			eq("abc"), eq("deployed"), eq(reply));
		org.junit.jupiter.api.Assertions.assertNull(result);
	}

	@Test
	void ignoresPlatformDownlinkBeforeBusinessDispatch() {
		VlsMqttMessageHandler handler = mock(VlsMqttMessageHandler.class);
		VlsMqttInboundDispatcher dispatcher = dispatcher(handler);

		dispatcher.dispatch("vlstream/v2.2/dev/CAM-1/bus",
			envelope("aiBiz", "modelDeploy", "{}", "platform2dev"));

		verifyNoInteractions(handler);
	}

	@Test
	void ignoresMessageWhenTopicDeviceDoesNotMatchEnvelope() {
		VlsMqttMessageHandler handler = mock(VlsMqttMessageHandler.class);
		VlsMqttInboundDispatcher dispatcher = dispatcher(handler);

		dispatcher.dispatch("vlstream/v2.2/dev/CAM-2/bus",
			envelope("device", "state", "{}", "dev2platform"));

		verifyNoInteractions(handler);
	}

	@Test
	void busPublishesOnlyTheReplyReturnedByDispatcher() throws Exception {
		VlsMqttInboundDispatcher dispatcher = mock(VlsMqttInboundDispatcher.class);
		VlsMqttBusService busService = spy(new VlsMqttBusService());
		setField(busService, "inboundDispatcher", dispatcher);
		JSONObject reply = new JSONObject();
		reply.put("messageId", "ack-1");
		when(dispatcher.dispatch("vlstream/v2.2/dev/CAM-1/bus", "request"))
			.thenReturn(reply);
		doNothing().when(busService).publish("vlstream/v2.2/dev/CAM-1/bus", reply);

		busService.handleIncomingMessage("vlstream/v2.2/dev/CAM-1/bus", "request");

		verify(busService).publish("vlstream/v2.2/dev/CAM-1/bus", reply);
		busService.destroy();
	}

	@Test
	void busStartsBackgroundConnectionMaintenance() {
		VlsMqttBusService busService = spy(new VlsMqttBusService());
		doNothing().when(busService).connectIfNecessary();

		busService.initialize();

		verify(busService, timeout(1000)).connectIfNecessary();
		busService.destroy();
	}

	private VlsMqttInboundDispatcher dispatcher(VlsMqttMessageHandler... handlers) {
		return new VlsMqttInboundDispatcher(Arrays.asList(handlers));
	}

	private String envelope(String mainBizType, String subBizType,
							String payload, String msgDir) {
		return "{"
			+ "\"protocolVersion\":\"2.2\","
			+ "\"messageId\":\"message-1\","
			+ "\"deviceId\":\"CAM-1\","
			+ "\"sentAt\":\"2026-08-13T00:00:00Z\","
			+ "\"msgDir\":\"" + msgDir + "\","
			+ "\"mainBizType\":\"" + mainBizType + "\","
			+ "\"subBizType\":\"" + subBizType + "\","
			+ "\"payload\":" + payload + ",\"extend\":{}}";
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Class<?> type = target.getClass();
		Field field = null;
		while (type != null && field == null) {
			try {
				field = type.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				type = type.getSuperclass();
			}
		}
		if (field == null) {
			throw new NoSuchFieldException(name);
		}
		field.setAccessible(true);
		field.set(target, value);
	}
}
