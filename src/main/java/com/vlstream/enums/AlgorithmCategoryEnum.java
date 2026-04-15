package com.vlstream.enums;

import lombok.Getter;

/**
 * 算法类型枚举
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmCategoryEnum {

    DETECT("detect", "目标检测算法"),
    SEGMENT("segment", "实例分割算法"),
	CLASSIFY("classify", "图像分类算法"),
	POSE("pose", "关键点检测算法"),
	OBB("obb", "旋转目标检测算法");

    private final String code;
    private final String description;

    AlgorithmCategoryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 枚举对象
     */
    public static AlgorithmCategoryEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (AlgorithmCategoryEnum algorithmCategoryEnum : values()) {
            if (algorithmCategoryEnum.getCode().equals(code)) {
                return algorithmCategoryEnum;
            }
        }
        return null;
    }
}
