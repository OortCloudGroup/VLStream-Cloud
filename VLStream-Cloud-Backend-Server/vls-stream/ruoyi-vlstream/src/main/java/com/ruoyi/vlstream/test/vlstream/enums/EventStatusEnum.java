/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 事件状态枚举
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
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
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
