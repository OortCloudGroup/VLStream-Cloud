/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * : task new .
 */
@Component
@ConditionalOnProperty(value = "vlstream.classify-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceClassifyDetectionRunner extends AbstractDetectionRunner<DeviceClassifyDetectionManager> {

	public DeviceClassifyDetectionRunner(ObjectProvider<DeviceClassifyDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
