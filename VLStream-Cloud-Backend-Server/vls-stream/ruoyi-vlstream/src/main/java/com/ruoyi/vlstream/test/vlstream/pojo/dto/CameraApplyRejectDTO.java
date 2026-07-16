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
 * 摄像头申请驳回参数
 */
@Data
public class CameraApplyRejectDTO {

	@Schema(description = "申请记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "申请记录ID不能为空")
	private Long id;

	@Schema(description = "审批人", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "审批人不能为空")
	private String approveUserName;

	@Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "驳回原因不能为空")
	private String approvalComment;
}
