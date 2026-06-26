package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Camera application approval status
 */
@Getter
public enum CameraApplyStatusEnum {

	pending("pending", "Pending approval"),
	approved("approved", "Passed"),
	rejected("rejected", "Rejected"),
	completed("completed", "Completed");

	@EnumValue
	private final String code;
	private final String description;

	CameraApplyStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}
}
