/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.rule.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * , collection, is in , each . object rule_condition_group
 *
 * @author ruoyi
 * @date 2024-12-20
 */
@Data
@ExcelIgnoreUnannotated
public class RuleConditionGroupVo {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "条件组ID")
    private String id;

    /**
     * id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * ID
     */
    @ExcelProperty(value = "关联的规则列表ID")
    private String ruleListId;

    /**
     *
     */
    @ExcelProperty(value = "条件组名称")
    private String name;

    /**
     *
     */
    @ExcelProperty(value = "条件组描述")
    private String description;

    /**
     *
     */
    @ExcelProperty(value = "或or且")
    private String andOr;
}
