package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsNativeDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("dev")
class WvpVlStreamDeviceResolverTest {

	@Test
	void springSelectsTheProductionConstructor() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getBeanFactory().registerSingleton("wvpVlStreamDeviceProperties", properties());
		context.getBeanFactory().registerSingleton("vlsNativeDeviceProperties", new VlsNativeDeviceProperties());
		context.getBeanFactory().registerSingleton("vlsEventReportApplicationService",
			mock(VlsEventReportApplicationService.class));
		context.register(WvpVlStreamDeviceResolver.class);
		try {
			context.refresh();
			assertNotNull(context.getBean(WvpVlStreamDeviceResolver.class));
		} finally {
			context.close();
		}
	}

	@Test
	void resolvesRegisteredOfflineDeviceFromWvp() {
		WvpVlStreamDeviceProperties properties = properties();
		VlsNativeDeviceProperties nativeProperties = new VlsNativeDeviceProperties();
		nativeProperties.setDefaultTenantId("000000");
		VlsEventReportApplicationService applicationService = mock(VlsEventReportApplicationService.class);
		when(applicationService.isMultiTenant()).thenReturn(false);
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		server.expect(requestTo("http://127.0.0.1:9080/internal/vlstream/device/CAM-1"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("{\"code\":200,\"data\":{\"deviceId\":\"CAM-1\","
				+ "\"deviceName\":\"WVP Camera\",\"online\":false}}", MediaType.APPLICATION_JSON));

		DeviceInfo device = new WvpVlStreamDeviceResolver(properties, nativeProperties,
			applicationService, restTemplate).resolve("CAM-1");

		assertEquals("CAM-1", device.getDeviceId());
		assertEquals("WVP Camera", device.getDeviceName());
		assertEquals("000000", device.getTenantId());
		server.verify();
	}

	@Test
	void rejectsDeviceMissingFromWvp() {
		WvpVlStreamDeviceProperties properties = properties();
		VlsNativeDeviceProperties nativeProperties = new VlsNativeDeviceProperties();
		VlsEventReportApplicationService applicationService = mock(VlsEventReportApplicationService.class);
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		server.expect(requestTo("http://127.0.0.1:9080/internal/vlstream/device/UNKNOWN"))
			.andRespond(withSuccess("{\"code\":404,\"msg\":\"VLStream设备不存在\"}",
				MediaType.APPLICATION_JSON));

		ServiceException exception = assertThrows(ServiceException.class,
			() -> new WvpVlStreamDeviceResolver(properties, nativeProperties,
				applicationService, restTemplate).resolve("UNKNOWN"));

		assertEquals("WVP不存在该VLStream设备：UNKNOWN", exception.getMessage());
		server.verify();
	}

	private WvpVlStreamDeviceProperties properties() {
		WvpVlStreamDeviceProperties properties = new WvpVlStreamDeviceProperties();
		properties.setBaseUrl("http://127.0.0.1:9080");
		return properties;
	}
}
