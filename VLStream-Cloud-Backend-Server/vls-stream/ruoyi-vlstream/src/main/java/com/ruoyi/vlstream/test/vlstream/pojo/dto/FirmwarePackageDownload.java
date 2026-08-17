/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

/** Authorized private-MinIO package stream returned through the platform short link. */
@Getter
@Builder
public class FirmwarePackageDownload {
	private final InputStream inputStream;
	private final String fileName;
	private final String contentType;
	private final long fileSize;
	private final String sha256;
}
