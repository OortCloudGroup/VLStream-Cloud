/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * : task new .
 */
@Component
@ConditionalOnProperty(value = "vlstream.face-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceFaceDetectionRunner extends AbstractDetectionRunner<DeviceFaceDetectionManager> {

	public DeviceFaceDetectionRunner(ObjectProvider<DeviceFaceDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
