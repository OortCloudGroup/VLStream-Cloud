/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.detection;

/**
 * device will : Check configurationwhether , will / .
 */
public interface DeviceDetectionSession {

	boolean matches(Long algorithmId, String streamUrl, String modelSourcePath);

	boolean start();

	void stop(String reason);
}
