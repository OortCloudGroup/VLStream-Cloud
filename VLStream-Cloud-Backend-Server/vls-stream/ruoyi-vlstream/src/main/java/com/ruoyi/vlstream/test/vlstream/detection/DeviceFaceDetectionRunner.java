/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 人脸检测启动触发器：应用启动时触发一次人脸检测任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.face-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceFaceDetectionRunner extends AbstractDetectionRunner<DeviceFaceDetectionManager> {

	public DeviceFaceDetectionRunner(ObjectProvider<DeviceFaceDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
