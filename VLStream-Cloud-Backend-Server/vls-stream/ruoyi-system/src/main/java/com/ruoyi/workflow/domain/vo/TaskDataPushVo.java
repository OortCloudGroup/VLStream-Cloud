/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * nodeinfoPush vo
 */
@Data
public class TaskDataPushVo {

    /**
     * taskid
     */
    private String taskId;

    /**
     * task
     */
    private String taskName;

    /**
     * taskassignee
     */
    private String assignee;

    /**
     * taskassigneewhether administrator(1 departmentleader 2 administrator )
     */
    private String assigneeType;

    /**
     * taskassigneeID card number
     */
    private String assigneeIdCard;

    /**
     * workflow instanceid
     */
    private String processInstanceId;

    /**
     *
     */
    private String fullMessage;

    /**
     * operation
     */
    private String operateType;

    /**
     * nodeextension properties
     */
    private List<Map<String, Object>> formDataMap;

    /**
     * nodeapprover
     */
    private String nextUserIds;
}
