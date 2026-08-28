/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workorder.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * work orderobject work_order
 *
 * @author
 * @date 2025-01-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * work orderprimary key ID
     */
    @TableId(value = "id")
    private String id;
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
    private String systemId;
    /**
     * item
     */
    private String projectId;
    /**
     * work order(workflow)
     */
    private String workorderId;
    /**
     * workflowkey
     */
    private String processKey;
    /**
     * work order
     */
    private String workorderNumber;
    /**
     * work order
     */
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
     * Delete , 0 not Delete , 1 Delete
     */
    @TableLogic
    private String delFlag;
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
     * event
     */
    private String eventNumber;
    /**
     * workflow
     */
    private String processName;
}
