package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Annotation Status Enum
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmAnnotationStatusEnum {

	none("none", "Unannotated"),
	partial("partial", "Partial labeling"),
	completed("completed", "Complete annotation");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmAnnotationStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static AlgorithmAnnotationStatusEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AlgorithmAnnotationStatusEnum algorithmAnnotationStatusEnum : values()) {
			if (algorithmAnnotationStatusEnum.getCode().equals(code)) {
				return algorithmAnnotationStatusEnum;
			}
		}
		return null;
	}
}
