/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workorder.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * work order object work_order
 *
 * @author
 * @date 2025-01-02
 */

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkOrderBo extends BaseEntity {
    private String id;
    /**
     *
     */
    private String systemId;

    /**
     * user ID
     */
    private String userId;

    /**
     * item
     */
    private String projectId;

    /**
     * work order(workflow)
     */
    @NotBlank(message = "工单类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String workorderId;

    /**
     * workflowid
     */
    @NotBlank(message = "流程名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processKey;

    /**
     * work order
     */
    private String workorderNumber;

    /**
     * work order
     */
    @NotBlank(message = "工单标题不能为空", groups = { AddGroup.class, EditGroup.class })
    private String title;

    /**
     * work order
     */
    private String description;

    /**
     * work order
     */
    private String workorderStatus;

    /**
     * work order
     */
    private String priority;

    /**
     * approval
     */
    private String processStatus;

    /**
     * work order
     */
    private String source;

    /**
     * whether
     */
    private String compensation;

    /**
     *
     */
    private String evaluate;

    /**
     *
     */
    private String roomNumber;

    /**
     * (JSON )
     */
    private String attachmentUrls;

    /**
     * work order(workflow) _ before
     */
    private String workorderIdExtend;
    /**
     * workflow instanceid
     */
    private String procInsId;

    /**
     * taskID
     */
    private String taskId;
    /**
     *
     */
    private String assignId;
    /**
     * work order
     */
    private String workOrderJobFlag;
    /**
     * work order
     */
    private String workOrderJobSerial;
    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderAppAll = false;
    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderSynthesisAll = false;
    /**
     * interface
     */
    private String apiPath;
    /**
     * event
     */
    private String eventNumber;
    /**
     * workflow
     */
    private String processName;

}
