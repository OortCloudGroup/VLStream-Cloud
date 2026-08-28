/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * workflow object
 *
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Data
public class WfDesignerBo {

    /**
     * workflow
     */
    @NotNull(message = "流程名称", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * workflow
     */
    @NotBlank(message = "流程分类", groups = { AddGroup.class, EditGroup.class })
    private String category;

    /**
     * XML
     */
    @NotBlank(message = "XML字符串", groups = { AddGroup.class, EditGroup.class })
    private String xml;
}
