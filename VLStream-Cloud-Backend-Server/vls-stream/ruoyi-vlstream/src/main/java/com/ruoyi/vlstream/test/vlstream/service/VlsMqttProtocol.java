/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import org.apache.commons.lang3.StringUtils;

/**
 * Fixed routing and message constants from VLS-Protocol.md.
 */
public final class VlsMqttProtocol {

	public static final String VERSION = "2.2";
	public static final String BUS_TOPIC_FILTER = "vlstream/v2.2/dev/+/bus";
	public static final String PLATFORM_TO_DEVICE = "platform2dev";
	public static final String DEVICE_TO_PLATFORM = "dev2platform";
	public static final String DEVICE_BIZ = "device";
	public static final String DEVICE_BIZ_COMPAT = "deviceBiz";
	public static final String STATE = "state";
	public static final String FIRMWARE_DEPLOY = "firmwareDeploy";
	public static final String AI_BIZ = "aiBiz";
	public static final String MODEL_DEPLOY = "modelDeploy";
	public static final String FACE_EVENT = "faceEvent";
	public static final String STRUCT_EVENT = "struct";

	private static final String BUS_TOPIC_PREFIX = "vlstream/v2.2/dev/";
	private static final String BUS_TOPIC_SUFFIX = "/bus";

	private VlsMqttProtocol() {
	}

	public static String deviceBusTopic(String deviceId) {
		String normalized = StringUtils.trimToEmpty(deviceId);
		if (StringUtils.isBlank(normalized)
			|| normalized.contains("/")
			|| normalized.contains("+")
			|| normalized.contains("#")) {
			throw new IllegalArgumentException("Invalid MQTT deviceId: " + deviceId);
		}
		return BUS_TOPIC_PREFIX + normalized + BUS_TOPIC_SUFFIX;
	}

	public static boolean isDeviceBiz(String mainBizType) {
		return StringUtils.equals(DEVICE_BIZ, mainBizType)
			|| StringUtils.equals(DEVICE_BIZ_COMPAT, mainBizType);
	}
}
