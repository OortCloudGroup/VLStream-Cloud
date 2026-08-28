/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;
import org.flowable.engine.task.Comment;

import java.util.List;


@Data
public class WfLastHisTaskInfoVo {
    /**
     * nodeid
     */
    private String taskId;
    /**
     * node
     */
    private String taskName;
    /**
     * approverid
     */
    private String assigneeId;
    /**
     * approver
     */
    private String assigneeName;
    /**
     *
     */
    private String commentMsg ;
    /**
     * operation
     */
    private String type ;
}
