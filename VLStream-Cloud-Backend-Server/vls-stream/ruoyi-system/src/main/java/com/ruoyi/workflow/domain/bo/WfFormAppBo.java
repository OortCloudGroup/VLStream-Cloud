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
 * form object wf_form_app
 *
 * @author
 * @date 2025-04-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormAppBo extends BaseEntity {

    /**
     * form id
     */
    private String categoryId;

    /**
     * ID
     */
    @NotBlank(message = "应用ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationId;

    /**
     *
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationName;

    /**
     *
     */
    @NotBlank(message = "应用密钥不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationSecret;


    /**
     * id
     */
    private String parentId;

    /**
     * form
     */
    private String categoryName;

    /**
     *
     */
    private String code;

    /**
     * 0 , 1
     */
    private String appFlag;
    /**
     * remark
     */
    private String remark;
    /**
     *
     */
    private String images;
    /**
     * 0workflow 1work order
     */
    private String type;
}
