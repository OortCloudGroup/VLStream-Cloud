package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 训练状态枚举
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
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
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
