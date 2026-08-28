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
 *
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmRepositoryTypeEnum {

	basic("basic", "基础预置"),
	extended("extended", "扩展");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmRepositoryTypeEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * codeGet
	 *
	 * @param code
	 * @return object
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
