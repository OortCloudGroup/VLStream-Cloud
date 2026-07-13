/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 规则树视图对象 rule_tree
 *
 * @author 雷超群
 * @date 2024-12-18
 */
@Data
@ExcelIgnoreUnannotated
public class RuleTreeVo {

    private static final long serialVersionUID = 1L;

    /**
     * 规则树ID
     */
    @ExcelProperty(value = "规则树ID")
    private String id;

    /**
     * 租户id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * 父节点ID, 根节点为NULL
     */
    @ExcelProperty(value = "父节点ID, 根节点为NULL")
    private String parentId;

    /**
     * 规则树名称
     */
    @ExcelProperty(value = "规则树名称")
    private String name;

    /**
     * 规则树描述
     */
    @ExcelProperty(value = "规则树描述")
    private String description;


}
