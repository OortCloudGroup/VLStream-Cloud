/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;

/** Metadata supplied before a direct firmware upload. */
@Data
public class FirmwareUploadRequest {
	private String cameraModel;
	private String firmwareVersion;
	private String fileName;
	private String contentType;
	private Long fileSize;
}
