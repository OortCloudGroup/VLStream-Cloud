/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.common.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatus {
    /**
     *
     */
    PENDING_DISPATCH("pendingDispatch"),

    /**
     *
     */
    PENDING_ORDERS("pendingOrders"),

    /**
     * Process in
     */
    PROCESSING("processing"),

    /**
     * already
     */
    REFERRED("referred"),

    /**
     * already
     */
    RETURNED("Returned"),

    /**
     *
     */
    RETURN("return"),

    /**
     * already
     */
    COMPLETED("completed"),

    /**
     * already
     */
    CLOSED("closed"),


    /**
     *
     */
    TO_BE_EVALUATED("toBeEvaluated");

    private final String status;

    public static WorkOrderStatus getWorkOrderStatus(String str) {
        if (StringUtils.isNotBlank(str)) {
            for (WorkOrderStatus value : values()) {
                if (StringUtils.equalsIgnoreCase(str, value.getStatus())) {
                    return value;
                }
            }
        }
        return null;
    }
    }
