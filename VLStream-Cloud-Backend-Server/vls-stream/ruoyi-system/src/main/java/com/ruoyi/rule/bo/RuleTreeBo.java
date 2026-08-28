/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.bo;

import com.ruoyi.common.core.domain.TreeEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * object rule_tree
 *
 * @author
 * @date 2024-12-18
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleTreeBo extends TreeEntity<RuleTreeBo> {

    private String id;

    /**
     * id
     */
    private String tenantId;

    /**
     * user ID
     */
    private String userId;

    /**
     *
     */
    @NotBlank(message = "规则树名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     *
     */
    private String description;
    /**
     * 0workflow 1work order
     */
    private String type;

}
