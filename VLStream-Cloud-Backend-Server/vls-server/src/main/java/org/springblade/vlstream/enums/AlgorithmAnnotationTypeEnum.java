package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Annotation Type Enum
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmAnnotationTypeEnum {

	rect("rect", "Rectangle"),
	circle("circle", "Circle"),
	polygon("polygon", "Polygon");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmAnnotationTypeEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static AlgorithmAnnotationTypeEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AlgorithmAnnotationTypeEnum algorithmAnnotationTypeEnum : values()) {
			if (algorithmAnnotationTypeEnum.getCode().equals(code)) {
				return algorithmAnnotationTypeEnum;
			}
		}
		return null;
	}
}
