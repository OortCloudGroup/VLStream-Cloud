/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * workflow objectExport VO
 *
 * @author konbai
 */
@Data
@NoArgsConstructor
public class WfOwnTaskExportVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * workflow instance ID
     */
    @ExcelProperty(value = "流程编号")
    private String procInsId;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程名称")
    private String procDefName;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程类别")
    private String category;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程版本")
    private int procDefVersion;

    /**
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "提交时间")
    private Date createTime;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程状态")
    private String status;

    /**
     * task
     */
    @ExcelProperty(value = "任务耗时")
    private String duration;

    /**
     * current node
     */
    @ExcelProperty(value = "当前节点")
    private String taskName;

    /**
     * task
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
}
