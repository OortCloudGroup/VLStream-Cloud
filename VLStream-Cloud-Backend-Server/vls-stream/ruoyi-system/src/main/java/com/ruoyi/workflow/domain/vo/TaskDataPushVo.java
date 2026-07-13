/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 节点信息推送vo
 */
@Data
public class TaskDataPushVo {

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务办理人
     */
    private String assignee;

    /**
     * 任务办理人是否管理员(1 部门领导   2 管理员审核)
     */
    private String assigneeType;

    /**
     * 任务办理人身份证号
     */
    private String assigneeIdCard;

    /**
     * 流程实例id
     */
    private String processInstanceId;

    /**
     * 意见
     */
    private String fullMessage;

    /**
     * 操作类型
     */
    private String operateType;

    /**
     * 节点扩展属性
     */
    private List<Map<String, Object>> formDataMap;

    /**
     * 下一节点审批人
     */
    private String nextUserIds;
}
