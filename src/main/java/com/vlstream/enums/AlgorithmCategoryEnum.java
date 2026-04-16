package com.vlstream.enums;

import lombok.Getter;

/**
 * Algorithm Type Enum
 *
 * @author Administrator
 */
@Getter
public enum AlgorithmCategoryEnum {

    DETECT("detect", "Object Detection Algorithm"),
    SEGMENT("segment", "Instance Segmentation Algorithm"),
	CLASSIFY("classify", "Image Classification Algorithm"),
	POSE("pose", "Keypoint Detection Algorithm"),
	OBB("obb", "Rotated Object Detection Algorithm");

    private final String code;
    private final String description;

    AlgorithmCategoryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get enum by code
     *
     * @param code Status code
     * @return Enum object
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
