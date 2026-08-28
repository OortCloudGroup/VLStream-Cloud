/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * event
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
	 * codeGet
	 *
	 * @param code
	 * @return object
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
