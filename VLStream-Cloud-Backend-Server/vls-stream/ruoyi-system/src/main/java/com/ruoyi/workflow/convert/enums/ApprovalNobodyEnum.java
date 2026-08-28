/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ApprovalNobodyEnum {
    REFUSE("refuse", "自动拒绝"),
    PASS("pass", "自动通过"),
    ADMIN("admin", "转交流程管理员"),
    ASSIGN("assign", "指定人员");

    @JsonValue
    private final String nobody;
    private final String description;

    ApprovalNobodyEnum(String nobody, String description) {
        this.nobody = nobody;
        this.description = description;
    }
}
