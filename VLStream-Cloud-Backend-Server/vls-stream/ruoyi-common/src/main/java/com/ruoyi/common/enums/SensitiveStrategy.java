/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.enums;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 *
 *
 * @author Yjoioooo
 * @version 3.6.0
 */
@AllArgsConstructor
public enum SensitiveStrategy {

    /**
     *
     */
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 3, 4)),

    /**
     *
     */
    PHONE(DesensitizedUtil::mobilePhone),

    /**
     *
     */
    ADDRESS(s -> DesensitizedUtil.address(s, 8)),

    /**
     *
     */
    EMAIL(DesensitizedUtil::email),

    /**
     *
     */
    BANK_CARD(DesensitizedUtil::bankCard);

    //

    private final Function<String, String> desensitizer;

    public Function<String, String> desensitizer() {
        return desensitizer;
    }
}
