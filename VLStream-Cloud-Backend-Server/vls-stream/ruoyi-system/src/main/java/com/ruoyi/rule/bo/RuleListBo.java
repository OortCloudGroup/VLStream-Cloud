/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * object rule_list
 *
 * @author ruoyi
 * @date 2024-12-18
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleListBo extends BaseEntity {

    private String id;

    /**
     * ID
     */
    @NotBlank(message = "关联的规则树ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String treeId;

    /**
     *
     */
    @NotBlank(message = "规则名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * , AviatorScript
     */
    private String expression;

    /**
     *
     */
    private String description;

    /**
     * , ACTIVE , INACTIVE
     */
    @NotBlank(message = "规则状态，ACTIVE表示生效，INACTIVE表示无效不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * formid
     */
    private String formId;
    /**
     * 0workflow 1work order
     */
    private String type;
}
