/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.condition;

import lombok.Data;

/**
 *
 */
@Data
public class Condition {
    private String field;
    private String operator;
    private Object value;
}
