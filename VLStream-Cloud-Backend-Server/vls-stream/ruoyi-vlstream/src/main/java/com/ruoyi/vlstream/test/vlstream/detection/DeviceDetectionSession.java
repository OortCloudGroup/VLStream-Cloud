/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

/**
 * 设备检测会话抽象：用于判断配置是否匹配，并管理会话的启动/停止生命周期。
 */
public interface DeviceDetectionSession {

	boolean matches(Long algorithmId, String streamUrl, String modelSourcePath);

	boolean start();

	void stop(String reason);
}
