package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DeviceEventMqttHandlerTest {

	private VlsDeviceInfoMapper deviceInfoMapper;
	private DeviceMediaUploadService mediaUploadService;
	private ActiveSafetyEventReportService activeSafetyEventReportService;
	private DeviceEventMqttHandler handler;

	@BeforeEach
	void setUp() throws Exception {
		deviceInfoMapper = mock(VlsDeviceInfoMapper.class);
		mediaUploadService = mock(DeviceMediaUploadService.class);
		activeSafetyEventReportService = mock(ActiveSafetyEventReportService.class);
		handler = new DeviceEventMqttHandler();
		setField(handler, "deviceInfoMapper", deviceInfoMapper);
		setField(handler, "mediaUploadService", mediaUploadService);
		setField(handler, "activeSafetyEventReportService", activeSafetyEventReportService);
	}

	@Test
	void storesStructEventOnlyAfterMediaValidation() {
		DeviceInfo device = new DeviceInfo();
		device.setDeviceId("CAM-1");
		device.setDeviceName("测试摄像头");
		device.setTag("重点区域");
		device.setAddress("测试位置");
		DeviceMediaUpload upload = new DeviceMediaUpload();
		upload.setMediaId("media-1");
		upload.setOssConfigKey("minio");
		upload.setObjectKey("events/CAM-1/2026/07/29/media-1.jpg");
		when(deviceInfoMapper.selectOne(any())).thenReturn(device);
		when(mediaUploadService.validateAndBind(
			"media-1", "CAM-1", upload.getObjectKey(), sha256(), "event-message-1"))
			.thenReturn(upload);
		when(activeSafetyEventReportService.report(any(ActiveSafetyEventReport.class))).thenReturn(
			ActiveSafetyEventReportResult.builder()
				.activeSafetyEventId("event-row-1")
				.mediaId("media-1")
				.build());

		JSONObject reply = handler.handle(eventMessage());

		assertEquals("SUCCESS", reply.getByPath("payload.bizData.status"));
		assertEquals("platform2dev", reply.getStr("msgDir"));
		ArgumentCaptor<ActiveSafetyEventReport> eventCaptor =
			ArgumentCaptor.forClass(ActiveSafetyEventReport.class);
		verify(activeSafetyEventReportService).report(eventCaptor.capture());
		ActiveSafetyEventReport stored = eventCaptor.getValue();
		assertEquals("event-message-1", stored.getSourceMessageId());
		assertEquals("device-event-1", stored.getDeviceEventId());
		assertEquals("media-1", stored.getMediaId());
		assertEquals("person_detected", stored.getEventType());
		assertEquals("设备上报结构化识别事件：person_detected", stored.getDescription());
		assertEquals("测试摄像头", stored.getDeviceName());
		assertEquals("重点区域", stored.getDeviceTag());
		assertEquals("测试位置", stored.getAddress());
		assertTrue(stored.getEventTime().getTime() > 0L);
	}

	@Test
	void returnsFailedReplyWhenMinioValidationFails() {
		DeviceInfo device = new DeviceInfo();
		device.setDeviceId("CAM-1");
		when(deviceInfoMapper.selectOne(any())).thenReturn(device);
		when(mediaUploadService.validateAndBind(any(), any(), any(), any(), any()))
			.thenThrow(new ServiceException("MinIO 图片尚未上传"));

		JSONObject reply = handler.handle(eventMessage());

		assertEquals("FAILED", reply.getByPath("payload.bizData.status"));
		assertEquals("MinIO 图片尚未上传", reply.getByPath("payload.errDetail"));
		verify(activeSafetyEventReportService, never()).report(any());
	}

	@Test
	void ignoresDuplicateBeforeValidatingMediaAgain() {
		when(activeSafetyEventReportService.findDuplicate(
			"event-message-1", "CAM-1", "device-event-1"))
			.thenReturn(ActiveSafetyEventReportResult.builder()
				.activeSafetyEventId("event-row-1")
				.mediaId("media-1")
				.duplicate(true)
				.build());

		JSONObject reply = handler.handle(eventMessage());

		assertEquals("SUCCESS", reply.getByPath("payload.bizData.status"));
		assertEquals("事件已处理，重复消息已忽略", reply.getByPath("payload.msg"));
		verify(mediaUploadService, never()).validateAndBind(any(), any(), any(), any(), any());
		verify(activeSafetyEventReportService, never()).report(any());
	}

	private JSONObject eventMessage() {
		return JSONUtil.parseObj("{"
			+ "\"protocolVersion\":\"2.2\","
			+ "\"messageId\":\"event-message-1\","
			+ "\"deviceId\":\"CAM-1\","
			+ "\"sentAt\":\"2026-07-29T10:00:00Z\","
			+ "\"msgDir\":\"dev2platform\","
			+ "\"mainBizType\":\"aiBiz\","
			+ "\"subBizType\":\"struct\","
			+ "\"payload\":{"
			+ "\"eventId\":\"device-event-1\","
			+ "\"eventType\":\"person_detected\","
			+ "\"type\":\"human\","
			+ "\"eventTime\":\"2026-07-29T10:00:00Z\","
			+ "\"media\":[{"
			+ "\"mediaId\":\"media-1\","
			+ "\"objectKey\":\"events/CAM-1/2026/07/29/media-1.jpg\","
			+ "\"sha256\":\"" + sha256() + "\"}]},"
			+ "\"extend\":{}}");
	}

	private String sha256() {
		return "b84bf7c0392a8dcabd8da974590997778eefc19baea824480883aea9384c3639";
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
