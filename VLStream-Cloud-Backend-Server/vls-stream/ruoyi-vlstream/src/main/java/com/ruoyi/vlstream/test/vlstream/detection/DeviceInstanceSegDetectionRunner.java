/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 实例分割启动触发器：应用启动时触发一次实例分割任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.instance-seg-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceInstanceSegDetectionRunner extends AbstractDetectionRunner<DeviceInstanceSegDetectionManager> {

	public DeviceInstanceSegDetectionRunner(ObjectProvider<DeviceInstanceSegDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
