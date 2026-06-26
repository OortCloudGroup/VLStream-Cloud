package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Camera application approval pass parameters
 */
@Data
public class CameraApplyApproveDTO {

	@Schema(description = "Application record ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Application record ID cannot be empty")
	private Long id;

	@Schema(description = "Approver", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Approver cannot be empty")
	private String approveUserName;

	@Schema(description = "Approval opinion")
	private String approvalComment;
}
