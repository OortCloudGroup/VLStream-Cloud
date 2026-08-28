/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * already workflow objectExport VO
 *
 * @author konbai
 */
@Data
@NoArgsConstructor
public class WfFinishedTaskExportVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * task
     */
    @ExcelProperty(value = "任务编号")
    private String taskId;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程名称")
    private String procDefName;

    /**
     * tasknode
     */
    @ExcelProperty(value = "任务节点")
    private String taskName;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程版本")
    private int procDefVersion;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程发起人")
    private String startUserName;

    /**
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "接收时间")
    private Date createTime;

    /**
     * approval
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "审批时间")
    private Date finishTime;

    /**
     * task
     */
    @ExcelProperty(value = "任务耗时")
    private String duration;
}
