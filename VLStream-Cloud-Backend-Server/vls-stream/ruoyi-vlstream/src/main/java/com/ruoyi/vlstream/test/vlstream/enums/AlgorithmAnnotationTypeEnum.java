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
 * annotation
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmAnnotationTypeEnum {

	rect("rect", "矩形"),
	circle("circle", "圆形"),
	polygon("polygon", "多边形");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmAnnotationTypeEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * codeGet
	 *
	 * @param code
	 * @return object
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
