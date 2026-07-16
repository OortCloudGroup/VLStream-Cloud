/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 摄像头申请提交参数
 */
@Data
public class CameraApplySubmitDTO {

	@Schema(description = "设备主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "设备主键ID不能为空")
	private Long deviceInfoId;

	@Schema(description = "申请原因", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "申请原因不能为空")
	private String applyReason;

	@Schema(description = "申请备注")
	private String applyRemark;

	@Schema(description = "申请人", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "申请人不能为空")
	private String applyUserName;
}
