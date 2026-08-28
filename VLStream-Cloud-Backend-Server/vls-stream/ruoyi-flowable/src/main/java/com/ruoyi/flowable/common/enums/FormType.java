/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author KonBAI
 * @createTime 2022/6/28 9:51
 */
@Getter
@AllArgsConstructor
public enum FormType {

    /**
     * workflowform
     */
    PROCESS(0),

    /**
     * form
     */
    EXTERNAL(1),

    /**
     * node form
     */
    INDEPENDENT(2);

    /**
     * form
     */
    private final Integer type;
}
