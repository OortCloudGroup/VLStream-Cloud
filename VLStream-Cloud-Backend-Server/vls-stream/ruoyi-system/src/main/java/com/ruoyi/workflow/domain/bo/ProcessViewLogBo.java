/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * workflow log object process_view_log
 *
 * @author lcq
 * @date 2025-08-15
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessViewLogBo extends BaseEntity {

    /**
     * primary key
     */
    private String id;

    /**
     * workflow instanceid (processInstanceId)
     */
    private String processInstanceId;

    /**
     * workflow definition key (processKey)
     */
    private String processKey;

    /**
     * user ID
     */
    private String viewerUserId;

    /**
     * user /
     */
    private String viewerUsername;

    /**
     * department ID
     */
    private String viewerDeptId;

    /**
     * department name
     */
    private String viewerDeptName;

    /**
     * operation
     */
    private String operationType;

    /**
     * workflow
     */
    private String processStatus;

    /**
     *
     */
    private Date viewTime;

    /**
     *
     */
    private String attachmentName;

    /**
     * id
     */
    private String tenantId;

    /**
     * Delete , 0 not Delete , 1 Delete
     */
    private String delFlag;


}
