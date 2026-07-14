/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 流程图JSON业务对象 re_mode_json
 *
 * @author 雷超群
 * @date 2024-11-02
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ReModeJsonBo extends BaseEntity {

    /**
     * 与act_re_model 表的关联ID
     */
    @NotBlank(message = "与act_re_model 表的关联ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelId;

    /**
     * 租户id
     */
    @NotBlank(message = "租户id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tenantId;

    /**
     * 用户id
     */
    @NotBlank(message = "用户id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String userId;

    /**
     * 流程图JSON
     */
    @NotBlank(message = "流程图JSON不能为空", groups = { AddGroup.class, EditGroup.class })
    private String jsonContent;


}
