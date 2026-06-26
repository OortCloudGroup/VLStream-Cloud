package org.springblade.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Event level enum
 *
 * @author Administrator
 */
@Getter
public enum EventLevelEnum {

	low("low", "Low"),
	medium("medium", "Medium"),
	high("high", "High"),
	urgent("urgent", "Urgent");

	@EnumValue
	private final String code;
	private final String description;

	EventLevelEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get enum by code
	 *
	 * @param code status code
	 * @return enum object
	 */
	public static EventLevelEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (EventLevelEnum eventLevelEnum : values()) {
			if (eventLevelEnum.getCode().equals(code)) {
				return eventLevelEnum;
			}
		}
		return null;
	}
}
