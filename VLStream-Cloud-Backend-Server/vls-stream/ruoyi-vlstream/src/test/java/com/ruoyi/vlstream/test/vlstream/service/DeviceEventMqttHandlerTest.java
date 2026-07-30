package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
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
	private IVlsEventManagementService eventManagementService;
	private VlsEventManagementMapper eventMapper;
	private DeviceEventMqttHandler handler;

	@BeforeEach
	void setUp() throws Exception {
		deviceInfoMapper = mock(VlsDeviceInfoMapper.class);
		mediaUploadService = mock(DeviceMediaUploadService.class);
		eventManagementService = mock(IVlsEventManagementService.class);
		eventMapper = mock(VlsEventManagementMapper.class);
		handler = new DeviceEventMqttHandler();
		setField(handler, "deviceInfoMapper", deviceInfoMapper);
		setField(handler, "mediaUploadService", mediaUploadService);
		setField(handler, "eventManagementService", eventManagementService);
		setField(handler, "eventMapper", eventMapper);
	}

	@Test
	void storesStructEventOnlyAfterMediaValidation() {
		DeviceInfo device = new DeviceInfo();
		device.setDeviceId("CAM-1");
		device.setAddress("测试位置");
		DeviceMediaUpload upload = new DeviceMediaUpload();
		upload.setMediaId("media-1");
		upload.setOssConfigKey("minio");
		upload.setObjectKey("events/CAM-1/2026/07/29/media-1.jpg");
		when(deviceInfoMapper.selectOne(any())).thenReturn(device);
		when(mediaUploadService.validateAndBind(
			"media-1", "CAM-1", upload.getObjectKey(), sha256(), "event-message-1"))
			.thenReturn(upload);
		when(eventManagementService.createEvent(any(EventManagement.class))).thenReturn(true);

		JSONObject reply = handler.handle(eventMessage());

		assertEquals("SUCCESS", reply.getByPath("payload.bizData.status"));
		assertEquals("platform2dev", reply.getStr("msgDir"));
		ArgumentCaptor<EventManagement> eventCaptor = ArgumentCaptor.forClass(EventManagement.class);
		verify(eventManagementService).createEvent(eventCaptor.capture());
		EventManagement stored = eventCaptor.getValue();
		assertEquals("event-message-1", stored.getMqttMessageId());
		assertEquals("device-event-1", stored.getDeviceEventId());
		assertEquals("media-1", stored.getMediaId());
		assertTrue(stored.getReportImg().contains(upload.getObjectKey()));
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
		verify(eventManagementService, never()).createEvent(any());
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
