/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * approval parameter
 */
@Data
public class CameraApplyApproveDTO {

	@Schema(description = "申请记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "申请记录ID不能为空")
	private Long id;

	@Schema(description = "审批人", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "审批人不能为空")
	private String approveUserName;

	@Schema(description = "审批意见")
	private String approvalComment;
}
