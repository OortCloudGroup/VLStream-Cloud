package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Repository type enum
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmRepositoryTypeEnum {

	basic("basic", "Basic preset"),
	extended("extended", "Extension");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmRepositoryTypeEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static AlgorithmRepositoryTypeEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AlgorithmRepositoryTypeEnum algorithmRepositoryTypeEnum : values()) {
			if (algorithmRepositoryTypeEnum.getCode().equals(code)) {
				return algorithmRepositoryTypeEnum;
			}
		}
		return null;
	}
}
