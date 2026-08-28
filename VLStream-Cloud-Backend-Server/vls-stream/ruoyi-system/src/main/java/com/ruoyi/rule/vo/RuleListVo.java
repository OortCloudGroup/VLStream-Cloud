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
 * object rule_list
 *
 * @author ruoyi
 * @date 2024-12-18
 */
@Data
@ExcelIgnoreUnannotated
public class RuleListVo {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "规则ID")
    private String id;

    /**
     * ID
     */
    @ExcelProperty(value = "关联的规则树ID")
    private String treeId;

    /**
     *
     */
    @ExcelProperty(value = "规则名称")
    private String name;

    /**
     * , AviatorScript
     */
    @ExcelProperty(value = "规则表达式，使用AviatorScript语言")
    private String expression;

    /**
     *
     */
    @ExcelProperty(value = "规则描述")
    private String description;

    /**
     * , ACTIVE , INACTIVE
     */
    @ExcelProperty(value = "规则状态，ACTIVE表示生效，INACTIVE表示无效")
    private String status;

    /**
     * formid
     */
    @ExcelProperty(value = "表单id")
    private String formId;
}
