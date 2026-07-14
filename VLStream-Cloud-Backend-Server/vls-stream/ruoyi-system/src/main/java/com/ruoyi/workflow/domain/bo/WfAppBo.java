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
 * 应用通用流程业务对象 wf_app
 *
 * @author 雷超群
 * @date 2025-01-04
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfAppBo extends BaseEntity {

    /**
     * 主键ID
     */
    private String appId;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationName;

    /**
     * 应用ID
     */
    @NotBlank(message = "应用ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationId;

    /**
     * 应用密钥
     */
    @NotBlank(message = "应用密钥不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationSecret;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;

    /**
     * 图标地址
     */
    private String images;
    /**
     * 应用包名
     */
    private String appPackage;
}
