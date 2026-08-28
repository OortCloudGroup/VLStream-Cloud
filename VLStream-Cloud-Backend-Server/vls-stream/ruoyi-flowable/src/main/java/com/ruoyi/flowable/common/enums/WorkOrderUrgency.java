/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.flowable.common.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderUrgency {
    /**
     *
     */
    NORMAL("normal"),

    /**
     *
     */
    URGENT("urgent"),

    /**
     *
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
