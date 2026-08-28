/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.condition;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class FilterRules {
    private String operator;//
    private List<Condition> conditions;
    private List<FilterRules> groups;
}
