package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Event status enum
 *
 * @author Administrator
 */
@Getter
public enum EventStatusEnum {

	pending("pending", "Pending"),
	processing("processing", "Processing"),
	completed("completed", "Completed"),
	closed("closed", "Closed");

	@EnumValue
	private final String code;
	private final String description;

	EventStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static EventStatusEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (EventStatusEnum eventStatusEnum : values()) {
			if (eventStatusEnum.getCode().equals(code)) {
				return eventStatusEnum;
			}
		}
		return null;
	}
}
