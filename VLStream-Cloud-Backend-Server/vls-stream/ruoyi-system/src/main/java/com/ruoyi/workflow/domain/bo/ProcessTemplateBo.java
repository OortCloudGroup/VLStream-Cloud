/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * workflowInitialize object process_template
 *
 * @author lcq
 * @date 2025-01-07
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessTemplateBo extends BaseEntity {

    /**
     * ID
     */
    private String id;

    /**
     * id
     */
    private String deploymentId;

    /**
     * modelid
     */
    private String modelId;

    /**
     * modelKey
     */
    private String modelKey;

    /**
     * model
     */
    private String modelName;

    /**
     * whether 0 ( ) 1 ( )
     */
    private String showMobile;

    /**
     * id
     */
    private String tenantId;

    /**
     * user ID
     */
    private String userId;

    /**
     *
     */
    private String description;
}
