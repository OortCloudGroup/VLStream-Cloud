/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * annotation
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmAnnotationStatusEnum {

	none("none", "未标注"),
	partial("partial", "部分标注"),
	completed("completed", "完成标注");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmAnnotationStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * codeGet
	 *
	 * @param code
	 * @return object
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
