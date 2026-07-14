/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
