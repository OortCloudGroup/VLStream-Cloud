/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 请求状态枚举
 *
 * @author Administrator
 */
@Getter
public enum AnalysisRequestStatusEnum {

	cancel("cancel", "取消"),
	processing("processing", "分析中"),
	completed("completed", "已完成"),
	failed("failed", "已失败");

	@EnumValue
	private final String code;
	private final String description;

	AnalysisRequestStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
	 */
	public static AnalysisRequestStatusEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AnalysisRequestStatusEnum analysisRequestStatusEnum : values()) {
			if (analysisRequestStatusEnum.getCode().equals(code)) {
				return analysisRequestStatusEnum;
			}
		}
		return null;
	}
}
