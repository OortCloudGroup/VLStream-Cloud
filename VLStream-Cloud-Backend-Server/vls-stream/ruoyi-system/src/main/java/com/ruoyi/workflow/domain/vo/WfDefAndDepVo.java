/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

@Data
public class WfDefAndDepVo {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    private String definitionId;


    /**
     * 部署ID
     */
    private String deploymentId;
}
