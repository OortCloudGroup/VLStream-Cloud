/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Short-lived direct-upload grant returned to a field device.
 */
@Data
@Builder
public class DeviceMediaUploadResponse {

	private String mediaId;
	private String objectKey;
	private String uploadUrl;
	private String expiresAt;
	private String requiredContentType;
}
