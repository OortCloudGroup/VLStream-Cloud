/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * 规则列视图对象 rule_list
 *
 * @author ruoyi
 * @date 2024-12-18
 */
@Data
@ExcelIgnoreUnannotated
public class RuleListVo {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @ExcelProperty(value = "规则ID")
    private String id;

    /**
     * 关联的规则树ID
     */
    @ExcelProperty(value = "关联的规则树ID")
    private String treeId;

    /**
     * 规则名称
     */
    @ExcelProperty(value = "规则名称")
    private String name;

    /**
     * 规则表达式，使用AviatorScript语言
     */
    @ExcelProperty(value = "规则表达式，使用AviatorScript语言")
    private String expression;

    /**
     * 规则描述
     */
    @ExcelProperty(value = "规则描述")
    private String description;

    /**
     * 规则状态，ACTIVE表示生效，INACTIVE表示无效
     */
    @ExcelProperty(value = "规则状态，ACTIVE表示生效，INACTIVE表示无效")
    private String status;

    /**
     * 表单id
     */
    @ExcelProperty(value = "表单id")
    private String formId;
}
