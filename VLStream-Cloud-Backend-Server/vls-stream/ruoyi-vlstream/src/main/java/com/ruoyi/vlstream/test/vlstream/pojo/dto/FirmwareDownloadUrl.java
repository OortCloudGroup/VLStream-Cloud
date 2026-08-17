/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

/** Temporary private firmware download URL. */
@Data
@Builder
public class FirmwareDownloadUrl {
	private String url;
	private String fileName;
	private String expiresAt;
}
