package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Request Status Enum
 *
 * @author Administrator
 */
@Getter
public enum AnalysisRequestStatusEnum {

	cancel("cancel", "Cancel"),
	processing("processing", "Analyzing"),
	completed("completed", "Completed"),
	failed("failed", "Failed");

	@EnumValue
	private final String code;
	private final String description;

	AnalysisRequestStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static AnalysisRequestStatusEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AnalysisRequestStatusEnum analysisRequestStatusEnum : values()) {
			if (analysisRequestStatusEnum.getCode().equals(code)) {
				return analysisRequestStatusEnum;
			}
		}
		return null;
	}
}
