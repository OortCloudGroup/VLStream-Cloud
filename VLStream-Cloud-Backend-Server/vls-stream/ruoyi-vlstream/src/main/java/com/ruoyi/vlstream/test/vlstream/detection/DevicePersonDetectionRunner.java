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
@ConditionalOnProperty(value = "vlstream.person-detection.enabled", havingValue = "true", matchIfMissing = true)
public class DevicePersonDetectionRunner extends AbstractDetectionRunner<DevicePersonDetectionManager> {

	public DevicePersonDetectionRunner(ObjectProvider<DevicePersonDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}

}
