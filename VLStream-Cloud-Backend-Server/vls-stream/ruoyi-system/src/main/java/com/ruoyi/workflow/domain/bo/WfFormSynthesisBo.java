/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.TreeEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 表单分类业务对象 wf_form_category
 *
 * @author 雷超群
 * @date 2024-12-25
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormSynthesisBo extends TreeEntity<WfFormSynthesisBo> {

    /**
     * 表单分类id
     */
    private String categoryId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 表单分类名称
     */
    @NotBlank(message = "表单分类名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String categoryName;

    /**
     * 分类编码
     */
    private String code;

    /**
     * 备注
     */
    private String remark;

    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 0流程 1工单
     */
    private String type;
}
