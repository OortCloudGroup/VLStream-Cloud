/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum LoginType {

    /**
     *
     */
    PASSWORD("user.password.retry.limit.exceed", "user.password.retry.limit.count"),

    /**
     *
     */
    SMS("sms.code.retry.limit.exceed", "sms.code.retry.limit.count"),

    /**
     *
     */
    EMAIL("email.code.retry.limit.exceed", "email.code.retry.limit.count"),

    /**
     *
     */
    XCX("", "");

    /**
     * prompt / tip
     */
    final String retryLimitExceed;

    /**
     * prompt / tip
     */
    final String retryLimitCount;
}
