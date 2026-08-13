package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.vlstream.test.vlstream.config.VlsNativeDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMessageMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/** Marks devices offline when their MQTT heartbeat expires. */
@Component
@RequiredArgsConstructor
public class MqttDeviceOfflineScheduler {
	private final MqttDeviceMapper deviceMapper;
	private final MqttDeviceMessageMapper messageMapper;
	private final VlsNativeDeviceProperties properties;

	@Scheduled(fixedDelayString = "${vlstream.native-device.offline-scan-millis:30000}")
	public void markExpiredDevicesOffline() {
		long timeoutMillis = Math.max(30L, properties.getOfflineTimeoutSeconds()) * 1000L;
		deviceMapper.update(null, new LambdaUpdateWrapper<MqttDevice>()
			.eq(MqttDevice::getOnline, true)
			.lt(MqttDevice::getLastHeartbeatTime, new Date(System.currentTimeMillis() - timeoutMillis))
			.set(MqttDevice::getOnline, false)
			.set(MqttDevice::getOnlineReason, "heartbeat_timeout")
			.set(MqttDevice::getUpdateTime, new Date()));
	}

	@Scheduled(cron = "${vlstream.native-device.message-cleanup-cron:0 15 3 * * ?}")
	public void cleanExpiredIdempotencyRecords() {
		long retentionMillis = Math.max(1L, properties.getMessageRetentionHours()) * 60L * 60L * 1000L;
		messageMapper.deleteBefore(new Date(System.currentTimeMillis() - retentionMillis));
	}
}
