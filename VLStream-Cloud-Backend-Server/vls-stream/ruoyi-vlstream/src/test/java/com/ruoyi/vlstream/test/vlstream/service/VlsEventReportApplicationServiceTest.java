package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsEventReportApplicationServiceTest {

	@Test
	void persistsEventAndOutboxForMultiTenantIngestion() throws Exception {
		IVlsEventManagementService eventService = mock(IVlsEventManagementService.class);
		VlsEventManagementMapper eventMapper = mock(VlsEventManagementMapper.class);
		EventReportOutboxMapper outboxMapper = mock(EventReportOutboxMapper.class);
		IFileUploadService fileUploadService = mock(IFileUploadService.class);
		VlsEventReportApplicationService service = new VlsEventReportApplicationService(
			eventService, eventMapper, outboxMapper, fileUploadService);
		setField(service, "tenantType", "multi");
		when(eventService.createEvent(any(EventManagement.class))).thenAnswer(invocation -> {
			EventManagement event = invocation.getArgument(0);
			event.setId(99L);
			return true;
		});
		when(outboxMapper.insert(any(EventReportOutbox.class))).thenReturn(1);

		DeviceInfo device = new DeviceInfo();
		device.setTenantId("tenant-a");
		device.setDeviceId("CAM-1");
		device.setDeviceName("camera");
		EventManagement event = new EventManagement();
		event.setReportDevice("CAM-1");
		event.setEventType("faceEvent");
		event.setEventDesc("face detected");
		event.setReportImg("vls-media://media-1");
		event.setMqttMessageId("message-1");
		event.setDeviceEventId("event-1");

		service.persistDeviceEvent(event, device);

		assertEquals("tenant-a", event.getTenantId());
		assertEquals(0, event.getIsReport());
		ArgumentCaptor<EventReportOutbox> captor = ArgumentCaptor.forClass(EventReportOutbox.class);
		verify(outboxMapper).insert(captor.capture());
		EventReportOutbox outbox = captor.getValue();
		assertEquals(99L, outbox.getEventId());
		assertEquals("tenant-a", outbox.getTenantId());
		assertEquals("vls-event:tenant-a:99", outbox.getIdempotencyKey());
		JSONObject payload = JSONUtil.parseObj(outbox.getPayloadJson());
		assertEquals("vls-media://media-1", payload.getJSONArray("pics").getStr(0));
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
