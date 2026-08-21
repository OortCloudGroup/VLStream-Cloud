package com.ruoyi.vlstream.test.vlstream.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Closes OTA tasks that can no longer make progress and releases their device lock. */
@Slf4j
@Component
@ConditionalOnProperty(value = "vlstream.native-device.legacy-enabled", havingValue = "true")
@RequiredArgsConstructor
public class FirmwareDeployTaskTimeoutScheduler {

	private final FirmwareDeployTaskService taskService;

	@Scheduled(fixedDelayString = "${vlstream.firmware.ota-task-timeout-scan-millis:30000}")
	public void expireStaleTasks() {
		int expired = taskService.expireStaleTasks();
		if (expired > 0) {
			log.warn("Marked {} stale VLS OTA task(s) as timed out", expired);
		}
	}
}
