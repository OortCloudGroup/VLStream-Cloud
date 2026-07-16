package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 算法类型枚举
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmCategoryEnum {
	personDetect("personDetect", "行人检测算法"),
	detect("detect", "目标检测算法"),
	segment("segment", "实例分割算法"),
	semanticSeg("semanticSeg", "语义分割算法"),
	classify("classify", "图像分类算法"),
	pose("pose", "关键点检测算法"),
	obb("obb", "旋转目标检测算法"),
	faceDetect("faceDetect", "人脸识别算法");

	@EnumValue
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
