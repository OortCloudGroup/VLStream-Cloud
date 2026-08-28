/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * work order object workorder_app
 *
 * @author
 * @date 2025-01-04
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderAppBo extends BaseEntity {

    /**
     * primary key ID
     */
    private String appId;

    /**
     *
     */
    private String applicationName;

    /**
     * ID
     */
    private String applicationId;

    /**
     *
     */
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
