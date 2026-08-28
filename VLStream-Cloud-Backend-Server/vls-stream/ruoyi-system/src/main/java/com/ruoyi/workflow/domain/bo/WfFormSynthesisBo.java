/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.TreeEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * form object wf_form_category
 *
 * @author
 * @date 2024-12-25
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormSynthesisBo extends TreeEntity<WfFormSynthesisBo> {

    /**
     * form id
     */
    private String categoryId;

    /**
     * user ID
     */
    private String userId;

    /**
     * form
     */
    @NotBlank(message = "表单分类名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String categoryName;

    /**
     *
     */
    private String code;

    /**
     * remark
     */
    private String remark;

    /**
     * id
     */
    private String tenantId;
    /**
     * 0workflow 1work order
     */
    private String type;
}
