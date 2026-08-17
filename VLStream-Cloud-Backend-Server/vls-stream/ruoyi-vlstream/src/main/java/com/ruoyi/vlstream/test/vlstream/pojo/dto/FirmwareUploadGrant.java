/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

/** Single-object MinIO upload permission returned to the browser. */
@Data
@Builder
public class FirmwareUploadGrant {
	private Long firmwareId;
	private String uploadUrl;
	private String requiredContentType;
	private String expiresAt;
}
