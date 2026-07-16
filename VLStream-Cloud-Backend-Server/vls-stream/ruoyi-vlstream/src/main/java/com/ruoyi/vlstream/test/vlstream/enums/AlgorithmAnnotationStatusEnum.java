package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 标注状态枚举
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
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
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
