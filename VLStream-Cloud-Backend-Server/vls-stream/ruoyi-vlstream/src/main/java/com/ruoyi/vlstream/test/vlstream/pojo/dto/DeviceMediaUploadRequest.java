/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request made by a field device before uploading an event image.
 */
@Data
public class DeviceMediaUploadRequest {

	@NotBlank
	private String deviceId;

	@NotBlank
	private String fileName;

	@NotBlank
	private String contentType;

	@NotNull
	private Long fileSize;

	@NotBlank
	private String sha256;
}
