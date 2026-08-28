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
 * workflow object wf_synthesis
 *
 * @author
 * @date 2025-01-04
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfSynthesisBo extends TreeEntity<WfSynthesisBo> {

    /**
     * primary key ID
     */
    private String synthesisId;

    /**
     *
     */
    @NotBlank(message = "分类名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String categoryName;

    /**
     * user ID
     */
    private String userId;

    /**
     * id
     */
    private String tenantId;
}
