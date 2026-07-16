/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 语义分割启动触发器：应用启动时触发一次语义分割任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.semseg-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceSemSegDetectionRunner extends AbstractDetectionRunner<DeviceSemSegDetectionManager> {

	public DeviceSemSegDetectionRunner(ObjectProvider<DeviceSemSegDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
