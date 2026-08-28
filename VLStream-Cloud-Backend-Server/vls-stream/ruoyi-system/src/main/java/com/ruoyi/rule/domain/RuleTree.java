/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * object rule_tree
 *
 * @author
 * @date 2024-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_tree")
public class RuleTree extends TreeEntity {
    private static final long serialVersionUID = 1L;

    /* * ID */
    @TableId(value = "id",type = IdType.ASSIGN_ID)
    private String id;
    /**
     * user ID
     */
    private String userId;
    /**
     * tenant ID
     */
    private String tenantId;
    /* * */
    @ExcelProperty("规则树名称")
    private String name;

    /* * */
    @ExcelProperty("规则树描述")
    private String description;

    /* * Delete , 0 not Delete , 1 Delete */
    private String delFlag;
    /**
     * 0workflow 1work order
     */
    private String type;
}
