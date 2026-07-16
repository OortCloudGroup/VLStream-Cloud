package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 标注类型枚举
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
	 * 根据code获取枚举
	 *
	 * @param code 状态码
	 * @return 枚举对象
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
