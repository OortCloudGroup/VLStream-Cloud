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
 * object rule_tree
 *
 * @author
 * @date 2024-12-18
 */
@Data
@ExcelIgnoreUnannotated
public class RuleTreeVo {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "规则树ID")
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
     * nodeID, node to NULL
     */
    @ExcelProperty(value = "父节点ID, 根节点为NULL")
    private String parentId;

    /**
     *
     */
    @ExcelProperty(value = "规则树名称")
    private String name;

    /**
     *
     */
    @ExcelProperty(value = "规则树描述")
    private String description;


}
