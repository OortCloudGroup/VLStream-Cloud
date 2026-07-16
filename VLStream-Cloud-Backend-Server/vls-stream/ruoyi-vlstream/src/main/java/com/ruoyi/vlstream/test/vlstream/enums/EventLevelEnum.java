package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 事件级别枚举
 *
 * @author Administrator
 */
@Getter
public enum EventLevelEnum {

	low("low", "低"),
	medium("medium", "中"),
	high("high", "高"),
	urgent("urgent", "紧急");

	@EnumValue
	private final String code;
	private final String description;

	EventLevelEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
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
