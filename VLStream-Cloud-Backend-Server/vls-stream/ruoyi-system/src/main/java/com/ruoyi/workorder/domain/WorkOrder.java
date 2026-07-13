/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workorder.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单对象 work_order
 *
 * @author 雷超群
 * @date 2025-01-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工单主键ID
     */
    @TableId(value = "id")
    private String id;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 所属系统
     */
    private String systemId;
    /**
     * 项目名称
     */
    private String projectId;
    /**
     * 工单(流程)类型
     */
    private String workorderId;
    /**
     * 关联的流程key
     */
    private String processKey;
    /**
     * 工单编号
     */
    private String workorderNumber;
    /**
     * 工单标题
     */
    private String title;
    /**
     * 工单描述
     */
    private String description;
    /**
     * 工单状态
     */
    private String workorderStatus;
    /**
     * 工单紧急程度
     */
    private String priority;
    /**
     * 审批状态
     */
    private String processStatus;
    /**
     * 工单来源
     */
    private String source;
    /**
     * 是否有偿
     */
    private String compensation;
    /**
     * 评价
     */
    private String evaluate;
    /**
     * 房号
     */
    private String roomNumber;
    /**
     * 删除标记，0表示未删除，1表示删除
     */
    @TableLogic
    private String delFlag;
    /**
     * 附件地址（JSON格式）
     */
    private String attachmentUrls;
    /**
     * 工单(流程)类型_用于前端回显
     */
    private String workorderIdExtend;
    /**
     * 流程实例id
     */
    private String procInsId;
    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 派单人
     */
    private String assignId;
    /**
     * 周期性工单标识
     */
    private String workOrderJobFlag;
    /**
     * 周期性工单统一序号
     */
    private String workOrderJobSerial;
    /**
     * 事件编号
     */
    private String eventNumber;
    /**
     * 流程名称
     */
    private String processName;
}
