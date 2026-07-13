/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.condition;

import lombok.Data;

import java.util.List;

/**
 * 筛选规则
 */
@Data
public class FilterRules {
    private String operator;//运算符
    private List<Condition> conditions;
    private List<FilterRules> groups;
}
