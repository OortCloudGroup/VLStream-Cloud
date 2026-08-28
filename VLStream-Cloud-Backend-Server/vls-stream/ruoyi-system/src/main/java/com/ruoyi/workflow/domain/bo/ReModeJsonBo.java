/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * workflow JSON object re_mode_json
 *
 * @author
 * @date 2024-11-02
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ReModeJsonBo extends BaseEntity {

    /**
     * and act_re_model ID
     */
    @NotBlank(message = "与act_re_model 表的关联ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelId;

    /**
     * after , .
     */
    private String tenantId;

    /**
     * user ID
     */
    @NotBlank(message = "用户id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String userId;

    /**
     * workflow JSON
     */
    @NotBlank(message = "流程图JSON不能为空", groups = { AddGroup.class, EditGroup.class })
    private String jsonContent;


}
