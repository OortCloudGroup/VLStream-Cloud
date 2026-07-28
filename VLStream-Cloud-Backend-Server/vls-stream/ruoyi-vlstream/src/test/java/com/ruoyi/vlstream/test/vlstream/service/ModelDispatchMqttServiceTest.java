package com.ruoyi.vlstream.test.vlstream.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
