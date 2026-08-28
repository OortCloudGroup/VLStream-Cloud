/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.flowable.common.enums;

/**
 * workflow
 *
 * @author Xuan xuan
 * @date 2021/4/19
 */
public enum FlowComment {

    /**
     *
     */
    NORMAL("1", "正常"),
    REBACK("2", "退回"),
    REJECT("3", "驳回"),
    DELEGATE("4", "委派"),
    TRANSFER("5", "转办"),
    STOP("6", "终止"),
    REVOKE("7", "撤回");

    /**
     *
     */
    private final String type;

    /**
     *
     */
    private final String remark;

    FlowComment(String type, String remark) {
        this.type = type;
        this.remark = remark;
    }

    public String getType() {
        return type;
    }

    public String getRemark() {
        return remark;
    }

    public static String getRemarkByType(String type) {
        for (FlowComment comment : FlowComment.values()) {
            if (comment.getType().equals(type)) {
                String remark = comment.getRemark();
                return remark;
            }
        }
        return null;
    }

}
