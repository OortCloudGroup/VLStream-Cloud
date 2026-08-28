/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 * workflow object wf_app
 *
 * @author
 * @date 2025-01-04
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfAppBo extends BaseEntity {

    /**
     * primary key ID
     */
    private String appId;

    /**
     *
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationName;

    /**
     * ID
     */
    @NotBlank(message = "应用ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationId;

    /**
     *
     */
    @NotBlank(message = "应用密钥不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationSecret;

    /**
     * user ID
     */
    private String userId;

    /**
     * 0 , 1
     */
    private String appFlag;

    /**
     *
     */
    private String images;
    /**
     *
     */
    private String appPackage;
}
