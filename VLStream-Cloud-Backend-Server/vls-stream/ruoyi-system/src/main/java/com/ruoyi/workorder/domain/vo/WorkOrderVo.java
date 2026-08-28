/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workorder.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

import java.util.Date;

/**
 * work order object work_order
 *
 * @author
 * @date 2025-01-02
 */
@Data
@ExcelIgnoreUnannotated
public class WorkOrderVo {

    private static final long serialVersionUID = 1L;

    /**
     * work orderprimary key ID
     */
    @ExcelProperty(value = "工单主键ID")
    private String id;

    /**
     * id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     *
     */
    @ExcelProperty(value = "所属系统")
    private String systemId;

    /**
     * item
     */
    @ExcelProperty(value = "项目名称")
    private String projectId;

    /**
     * work order(workflow)
     */
    @ExcelProperty(value = "工单(流程)类型")
    private String workorderId;

    /**
     * workflowkey
     */
    @ExcelProperty(value = "关联的流程key")
    private String processKey;

    /**
     * work order
     */
    @ExcelProperty(value = "工单编号")
    private String workorderNumber;

    /**
     * work order
     */
    @ExcelProperty(value = "工单标题")
    private String title;

    /**
     * work order
     */
    @ExcelProperty(value = "工单描述")
    private String description;

    /**
     * work order
     */
    @ExcelProperty(value = "工单状态")
    private String workorderStatus;

    /**
     * work order
     */
    @ExcelProperty(value = "工单紧急程度")
    private String priority;

    /**
     * approval
     */
    @ExcelProperty(value = "审批状态")
    private String processStatus;

    /**
     * work order
     */
    @ExcelProperty(value = "工单来源")
    private String source;

    // /**
    // * whether
    // */
    // @ExcelProperty(value = "whether ")
    // private String compensation;
    //
    // /**
    // *
    // */
    // @ExcelProperty(value = " ")
    // private String evaluate;

    // /**
    // *
    // */
    // @ExcelProperty(value = " ")
    // private String roomNumber;

    /**
     *
     */
    @ExcelProperty(value = "创建人")
    private String createBy;

    /**
     * create time
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * Update
     */
    @ExcelProperty(value = "修改时间")
    private Date updateTime;

    /**
     * (JSON )
     */
    @ExcelProperty(value = "附件地址", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "JSON格式")
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
     * workflow instanceid
     */
    private String procInstId;

    /**
     * taskID
     */
    private String taskId;
    /**
     *
     */
    private String assignId;
    /**
     * workflow
     */
    @ExcelProperty(value = "流程版本")
    private int procDefVersion;

    /**
     * task
     */
    private String taskName;

    /**
     * workflow definition
     */
    private String procDefName;
    /**
     * work order
     */
    private String categoryName;
    /**
     * workflow Id
     */
    private String startUserId;
    /**
     * workflow
     */
    private String startUserName;
    /**
     * work order
     */
    private String workOrderJobFlag;
    /**
     * work order
     */
    private String workOrderJobSerial;
    /**
     * workflowfinish
     */
    private String endTime;
    // /**
    // * id
    // */
    // private String iconId;

    /**
     * department name
     */
    private String deptName;
    /**
     * current Process
     */
    private String currentAssignName;
    /**
     * approvalnode
     */
    private String currentActivityName;
    /**
     * Process
     */
    private String processingTime;
    /**
     * event
     */
    private String eventNumber;
    /**
     * workflow
     */
    private String processName;
}
