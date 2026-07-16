/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 仓库类型枚举
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
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
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
