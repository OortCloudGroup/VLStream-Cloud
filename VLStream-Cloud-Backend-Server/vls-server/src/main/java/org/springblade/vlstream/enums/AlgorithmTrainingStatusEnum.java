package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Training Status Enum
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmTrainingStatusEnum {

	pending("pending", "Wait"),
	training("training", "Training"),
	completed("completed", "Completed"),
	failed("failed", "Failure"),
	stop("stop", "Stop");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmTrainingStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static AlgorithmTrainingStatusEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AlgorithmTrainingStatusEnum algorithmTrainingStatusEnum : values()) {
			if (algorithmTrainingStatusEnum.getCode().equals(code)) {
				return algorithmTrainingStatusEnum;
			}
		}
		return null;
	}
}
