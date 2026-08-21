package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DeviceMediaUploadServiceTest {

	@Test
	void rejectsUploadWhenDeviceIsMissingFromWvp() throws Exception {
		VlsDeviceMediaProperties properties = new VlsDeviceMediaProperties();
		properties.setAllowUnauthenticated(true);
		WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
		when(resolver.resolve("UNKNOWN")).thenThrow(new ServiceException("WVP不存在该VLStream设备：UNKNOWN"));

		DeviceMediaUploadService service = new DeviceMediaUploadService();
		setField(service, "properties", properties);
		setField(service, "wvpDeviceResolver", resolver);

		DeviceMediaUploadRequest request = new DeviceMediaUploadRequest();
		request.setDeviceId("UNKNOWN");
		request.setFileName("capture.jpg");
		request.setContentType("image/jpeg");
		request.setFileSize(1024L);
		request.setSha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.issueUploadUrl(request));

		assertEquals("WVP不存在该VLStream设备：UNKNOWN", exception.getMessage());
		verify(resolver).resolve("UNKNOWN");
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
