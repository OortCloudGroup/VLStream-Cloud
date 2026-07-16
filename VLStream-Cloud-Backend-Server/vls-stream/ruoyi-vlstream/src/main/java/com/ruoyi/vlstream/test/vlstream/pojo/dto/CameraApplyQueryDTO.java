package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 摄像头申请分页查询参数
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
