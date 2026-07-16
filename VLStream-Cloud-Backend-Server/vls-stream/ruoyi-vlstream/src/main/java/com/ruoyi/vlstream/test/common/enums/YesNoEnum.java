package com.ruoyi.vlstream.test.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Yes/no value used by the original VLS database columns.
 */
@Getter
public enum YesNoEnum {
    NO(0, "No"),
    YES(1, "Yes");

    @EnumValue
    private final Integer code;
    private final String description;

    YesNoEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Resolves an enum value from its persisted integer code.
     */
    public static YesNoEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (YesNoEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
