/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.condition;

import lombok.Data;

/**
 * 筛选条件
 */
@Data
public class Condition {
    private String field;
    private String operator;
    private Object value;
}
