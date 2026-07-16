/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 目标检测启动触发器：应用启动时触发一次目标检测任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.object-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceObjectDetectionRunner extends AbstractDetectionRunner<DeviceObjectDetectionManager> {

	public DeviceObjectDetectionRunner(ObjectProvider<DeviceObjectDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
