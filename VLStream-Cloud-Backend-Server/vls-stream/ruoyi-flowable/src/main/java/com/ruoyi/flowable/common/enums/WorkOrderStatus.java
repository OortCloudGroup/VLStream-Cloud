package com.ruoyi.flowable.common.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatus {
    /**
     * 待派单
     */
    PENDING_DISPATCH("pendingDispatch"),

    /**
     * 待接单
     */
    PENDING_ORDERS("pendingOrders"),

    /**
     * 处理中
     */
    PROCESSING("processing"),

    /**
     * 已转办
     */
    REFERRED("referred"),

    /**
     * 已退回
     */
    RETURNED("Returned"),

    /**
     * 待回访
     */
    RETURN("return"),

    /**
     * 已完成
     */
    COMPLETED("completed"),

    /**
     * 已关闭
     */
    CLOSED("closed"),


    /**
     * 待评价
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
