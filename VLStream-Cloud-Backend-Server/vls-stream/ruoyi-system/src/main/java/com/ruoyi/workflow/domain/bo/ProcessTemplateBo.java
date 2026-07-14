/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程初始化模版业务对象 process_template
 *
 * @author lcq
 * @date 2025-01-07
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessTemplateBo extends BaseEntity {

    /**
     * 模板ID
     */
    private String id;

    /**
     * 部署id
     */
    private String deploymentId;

    /**
     * 模型id
     */
    private String modelId;

    /**
     * 模型Key
     */
    private String modelKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 手机端是否显示 0（显示） 1（不显示）
     */
    private String showMobile;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 描述
     */
    private String description;
}
