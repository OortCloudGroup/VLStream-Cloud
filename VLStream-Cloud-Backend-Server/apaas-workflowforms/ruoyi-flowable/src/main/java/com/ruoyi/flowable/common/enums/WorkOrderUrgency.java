package com.ruoyi.flowable.common.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderUrgency {
    /**
     * 一般
     */
    NORMAL("normal"),

    /**
     * 紧急
     */
    URGENT("urgent"),

    /**
     * 严重
     */
    CRITICAL("critical");

    private final String status;

    public static WorkOrderUrgency getWorkOrderUrgency(String str) {
        if (StringUtils.isNotBlank(str)) {
            for (WorkOrderUrgency value : values()) {
                if (StringUtils.equalsIgnoreCase(str, value.getStatus())) {
                    return value;
                }
            }
        }
        return null;
    }
}
