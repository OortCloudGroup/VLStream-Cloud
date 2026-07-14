/*
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
     * 节点id
     */
    private String taskId;
    /**
     * 节点名称
     */
    private String taskName;
    /**
     * 审批人id
     */
    private String assigneeId;
    /**
     * 审批人名称
     */
    private String assigneeName;
    /**
     * 评论
     */
    private String commentMsg ;
    /**
     * 操作类型
     */
    private String type ;
}
