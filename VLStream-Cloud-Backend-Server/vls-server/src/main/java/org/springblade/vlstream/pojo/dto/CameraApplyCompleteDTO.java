package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Camera application completion parameters
 */
@Data
public class CameraApplyCompleteDTO {

	@Schema(description = "Application record ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Application record ID cannot be empty")
	private Long id;

	@Schema(description = "Completed by", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Completed by cannot be empty")
	private String completeUserName;

	@Schema(description = "Completion remarks")
	private String completeRemark;
}
