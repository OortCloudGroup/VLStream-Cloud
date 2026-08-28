/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Query parameter
 */
@Data
public class CameraApplyQueryDTO {

	@Schema(description = "设备主键ID")
	private Long deviceInfoId;

	@Schema(description = "申请状态")
	private String applyStatus;

	@Schema(description = "申请人")
	private String applyUserName;
}
