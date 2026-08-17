/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Firmware metadata safe for management-page display. */
@Data
@Builder
public class DeviceFirmwareView {
	private Long id;
	private String cameraModel;
	private String target;
	private String firmwareVersion;
	private String originalFileName;
	private Long fileSize;
	private String sha256;
	private String uploadStatus;
	private Date createTime;
}
