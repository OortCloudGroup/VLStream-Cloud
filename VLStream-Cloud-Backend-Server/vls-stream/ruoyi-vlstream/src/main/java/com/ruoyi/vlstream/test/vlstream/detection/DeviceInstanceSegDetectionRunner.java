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
 * instance : instance task new .
 */
@Component
@ConditionalOnProperty(value = "vlstream.instance-seg-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceInstanceSegDetectionRunner extends AbstractDetectionRunner<DeviceInstanceSegDetectionManager> {

	public DeviceInstanceSegDetectionRunner(ObjectProvider<DeviceInstanceSegDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
