/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

@Data
public class WfDefAndDepVo {

    private static final long serialVersionUID = 1L;

    /**
     * workflow definition ID
     */
    private String definitionId;


    /**
     * ID
     */
    private String deploymentId;
}
