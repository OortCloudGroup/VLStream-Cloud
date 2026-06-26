package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Camera application submission parameters
 */
@Data
public class CameraApplySubmitDTO {

	@Schema(description = "Device Primary Key ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Device primary key ID cannot be empty")
	private Long deviceInfoId;

	@Schema(description = "Reason for application", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Reason for application cannot be empty")
	private String applyReason;

	@Schema(description = "Application notes")
	private String applyRemark;

	@Schema(description = "Applicant", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Applicant cannot be empty")
	private String applyUserName;
}
