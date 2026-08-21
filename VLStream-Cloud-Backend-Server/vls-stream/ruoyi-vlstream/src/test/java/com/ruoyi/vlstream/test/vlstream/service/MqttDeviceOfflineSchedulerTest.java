package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsNativeDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMessageMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("dev")
class MqttDeviceOfflineSchedulerTest {

	@Test
	void delegatesCrossTenantOfflineUpdateToExplicitMapperMethod() {
		MqttDeviceMapper deviceMapper = mock(MqttDeviceMapper.class);
		VlsNativeDeviceProperties properties = new VlsNativeDeviceProperties();
		properties.setOfflineTimeoutSeconds(180L);
		MqttDeviceOfflineScheduler scheduler = new MqttDeviceOfflineScheduler(
			deviceMapper, mock(MqttDeviceMessageMapper.class), properties);
		long beforeCall = System.currentTimeMillis();

		scheduler.markExpiredDevicesOffline();

		ArgumentCaptor<Date> expiredCaptor = ArgumentCaptor.forClass(Date.class);
		ArgumentCaptor<Date> nowCaptor = ArgumentCaptor.forClass(Date.class);
		verify(deviceMapper).markExpiredDevicesOffline(expiredCaptor.capture(), nowCaptor.capture());
		assertTrue(nowCaptor.getValue().getTime() >= beforeCall);
		assertTrue(nowCaptor.getValue().getTime() - expiredCaptor.getValue().getTime() == 180000L);
	}
}
