package org.springblade.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Common Yes/No enum class
 * 1-Yes, 0-No
 *
 * @author Administrator
 */
@Getter
public enum YesNoEnum {

	/**
	 * No
	 */
	NO(0, "No"),

	/**
	 * Yes
	 */
	YES(1, "Yes");

	@EnumValue
	private final Integer code;
	private final String description;

	YesNoEnum(Integer code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static YesNoEnum of(Integer code) {
		if (code == null) {
			return null;
		}
		for (YesNoEnum status : values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		return null;
	}

	/**
	 * Determine if it is "Yes"
	 *
	 * @param code status code
	 * @return whether it is "Yes"
	 */
	public static boolean isYes(Integer code) {
		return YES.getCode().equals(code);
	}

	/**
	 * Determine if it is "No"
	 *
	 * @param code status code
	 * @return whether it is "No"
	 */
	public static boolean isNo(Integer code) {
		return NO.getCode().equals(code);
	}
}
