package com.ruoyi.vlstream.test.vlstream.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class ModelDispatchMqttServiceTest {

	private ModelDispatchTaskService taskService;
	private ModelDispatchMqttService mqttService;

	@BeforeEach
	void setUp() throws Exception {
		taskService = mock(ModelDispatchTaskService.class);
		mqttService = new ModelDispatchMqttService();
		setField(mqttService, "taskService", taskService);
	}

	@Test
	void appliesV22ModelDeployReplyUsingBothCorrelationIds() {
		String reply = "{"
			+ "\"protocolVersion\":\"2.2\","
			+ "\"messageId\":\"ack-1\","
			+ "\"deviceId\":\"CAM-1\","
			+ "\"sentAt\":\"2026-07-24T09:33:45Z\","
			+ "\"msgDir\":\"dev2platform\","
			+ "\"mainBizType\":\"aiBiz\","
			+ "\"subBizType\":\"modelDeploy\","
			+ "\"payload\":{"
			+ "\"sourceMsgId\":\"mqtt-1\","
			+ "\"code\":200,"
			+ "\"msg\":\"deployed\","
			+ "\"errCode\":0,"
			+ "\"errDetail\":\"\","
			+ "\"bizData\":{\"requestId\":\"request-1\",\"status\":\"SUCCESS\","
			+ "\"fileSha256\":\"abc\",\"costMs\":1200}},"
			+ "\"extend\":{}}";
		when(taskService.applyHardwareReply(
			"mqtt-1", "request-1", "CAM-1",
			"SUCCESS", "abc", "deployed", reply)).thenReturn(true);

		mqttService.handleIncomingMessage("vlstream/v2.2/dev/CAM-1/bus", reply);

		verify(taskService).applyHardwareReply(
			eq("mqtt-1"), eq("request-1"), eq("CAM-1"),
			eq("SUCCESS"), eq("abc"), eq("deployed"), eq(reply));
	}

	@Test
	void ignoresPlatformDownlinkReceivedFromTheSharedBusSubscription() {
		String downlink = "{"
			+ "\"protocolVersion\":\"2.2\","
			+ "\"messageId\":\"mqtt-1\","
			+ "\"deviceId\":\"CAM-1\","
			+ "\"msgDir\":\"platform2dev\","
			+ "\"mainBizType\":\"aiBiz\","
			+ "\"subBizType\":\"modelDeploy\","
			+ "\"payload\":{},\"extend\":{}}";

		mqttService.handleIncomingMessage("vlstream/v2.2/dev/CAM-1/bus", downlink);

		verifyNoInteractions(taskService);
	}

	@Test
	void routesStructEventsToTheEventHandlerAndPublishesBusinessReply() throws Exception {
		DeviceEventMqttHandler eventHandler = mock(DeviceEventMqttHandler.class);
		ModelDispatchMqttService service = spy(new ModelDispatchMqttService());
		setField(service, "taskService", taskService);
		setField(service, "deviceEventHandler", eventHandler);
		cn.hutool.json.JSONObject reply = new cn.hutool.json.JSONObject();
		reply.put("messageId", "reply-1");
		when(eventHandler.handle(org.mockito.ArgumentMatchers.any())).thenReturn(reply);
		doNothing().when(service).publish("vlstream/v2.2/dev/CAM-1/bus", reply);
		String event = "{"
			+ "\"protocolVersion\":\"2.2\","
			+ "\"messageId\":\"event-1\","
			+ "\"deviceId\":\"CAM-1\","
			+ "\"msgDir\":\"dev2platform\","
			+ "\"mainBizType\":\"aiBiz\","
			+ "\"subBizType\":\"struct\","
			+ "\"payload\":{},\"extend\":{}}";

		service.handleIncomingMessage("vlstream/v2.2/dev/CAM-1/bus", event);

		verify(eventHandler).handle(org.mockito.ArgumentMatchers.any());
		verify(service).publish("vlstream/v2.2/dev/CAM-1/bus", reply);
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
