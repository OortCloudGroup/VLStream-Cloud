/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotifyTypeEnum {
    SITE("site", "站内"),
    EMAIL("email", "邮件"),
    SMS("sms", "短信"),
    WECHAT("wechat", "微信"),
    DINGTALK("dingtalk", "钉钉"),
    FEISHU("feishu", "飞书");

    @JsonValue
    private final String type;
    private final String description;
}
