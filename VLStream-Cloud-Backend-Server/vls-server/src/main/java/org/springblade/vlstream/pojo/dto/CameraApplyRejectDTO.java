package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Camera application rejection parameters
 */
@Data
public class CameraApplyRejectDTO {

	@Schema(description = "Application record ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Application record ID cannot be empty")
	private Long id;

	@Schema(description = "Approver", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Approver cannot be empty")
	private String approveUserName;

	@Schema(description = "Rejection reason", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Rejection reason cannot be empty")
	private String approvalComment;
}
