/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * workflowtask object
 *
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Data
public class WfTaskBo {
    /**
     * taskId
     */
    private String taskId;
    /**
     * task
     */
    private String taskName;
    /**
     * userId
     */
    private String userId;
    /**
     * task
     */
    private String comment;
    /**
     * 1 ,2 ,3
     */
    private String callbackType;
    /**
     *
     */
    private boolean acceptance;
    /**
     * workflow instanceId
     */
    private String procInsId;
    /**
     * node
     */
    private String targetKey;
    /**
     * workflow variableinfo
     */
    private Map<String, Object> variables;
    /**
     * approver
     */
    private String assignee;
    /**
     * candidate user
     */
    private List<String> candidateUsers;
    /**
     * approval
     */
    private List<String> candidateGroups;
    /**
     * userId
     */
    private String copyUserIds;
    /**
     * nodeapprover
     */
    private String nextUserIds;
    /**
     * userwhether need to Push
     */
    @JsonProperty(value = "PushMessage")
    private boolean  PushMessage;
}
