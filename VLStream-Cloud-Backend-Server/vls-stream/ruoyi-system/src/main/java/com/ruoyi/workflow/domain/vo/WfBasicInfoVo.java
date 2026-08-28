/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class WfBasicInfoVo {
    /**
     * workflow
     */
    private String processCategory;
    /**
     * workflow
     */
    private String processName;
    /**
     * workflow
     */
    private String processId;
    /**
     *
     */
    private Date submissionTime;
    /**
     * current task id
     */
    private String taskDefId;
}
