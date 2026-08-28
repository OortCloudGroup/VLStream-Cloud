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
public enum EventStatusEnum {

	pending("pending", "待处理"),
	processing("processing", "处理中"),
	completed("completed", "已完成"),
	closed("closed", "已关闭");

	@EnumValue
	private final String code;
	private final String description;

	EventStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * codeGet
	 *
	 * @param code
	 * @return object
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
