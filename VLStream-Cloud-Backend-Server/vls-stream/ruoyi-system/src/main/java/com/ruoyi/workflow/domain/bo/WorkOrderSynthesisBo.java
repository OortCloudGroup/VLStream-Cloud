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
 * 综合工单流程业务对象 workorder_synthesis
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderSynthesisBo extends TreeEntity<WorkOrderSynthesisBo> {

    /**
     * 主键ID
     */
    private String synthesisId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String categoryName;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 用户id
     */
    private String userId;


}
