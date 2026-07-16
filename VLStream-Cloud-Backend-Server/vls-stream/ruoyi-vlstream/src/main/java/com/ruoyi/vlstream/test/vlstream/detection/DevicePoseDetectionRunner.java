/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 姿态估计启动触发器：应用启动时触发一次姿态估计任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.pose-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DevicePoseDetectionRunner extends AbstractDetectionRunner<DevicePoseDetectionManager> {

	public DevicePoseDetectionRunner(ObjectProvider<DevicePoseDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
