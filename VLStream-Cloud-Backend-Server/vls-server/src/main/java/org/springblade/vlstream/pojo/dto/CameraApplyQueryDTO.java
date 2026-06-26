package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Camera application paging query parameters
 */
@Data
public class CameraApplyQueryDTO {

	@Schema(description = "Device Primary Key ID")
	private Long deviceInfoId;

	@Schema(description = "Application status")
	private String applyStatus;

	@Schema(description = "Applicant")
	private String applyUserName;
}
