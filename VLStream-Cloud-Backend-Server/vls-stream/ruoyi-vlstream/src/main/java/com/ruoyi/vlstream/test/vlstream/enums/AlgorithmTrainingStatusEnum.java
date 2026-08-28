/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * training
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmTrainingStatusEnum {

	pending("pending", "等待"),
	training("training", "训练中"),
	completed("completed", "完成"),
	failed("failed", "失败"),
	stop("stop", "停止");

	@EnumValue
	private final String code;
	private final String description;

	AlgorithmTrainingStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * codeGet
	 *
	 * @param code
	 * @return object
	 */
	public static AlgorithmTrainingStatusEnum of(String code) {
		if (code == null) {
			return null;
		}
		for (AlgorithmTrainingStatusEnum algorithmTrainingStatusEnum : values()) {
			if (algorithmTrainingStatusEnum.getCode().equals(code)) {
				return algorithmTrainingStatusEnum;
			}
		}
		return null;
	}
}
