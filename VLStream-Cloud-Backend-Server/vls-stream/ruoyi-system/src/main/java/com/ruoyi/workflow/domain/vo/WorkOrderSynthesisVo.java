/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 综合工单流程视图对象 workorder_synthesis
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
@Data
@ExcelIgnoreUnannotated
public class WorkOrderSynthesisVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private String synthesisId;

    /**
     * 父节点ID
     */
    @ExcelProperty(value = "父节点ID")
    private String parentId;

    /**
     * 分类名称
     */
    @ExcelProperty(value = "分类名称")
    private String categoryName;

    /**
     * 流程描述
     */
    @ExcelProperty(value = "流程描述")
    private String description;

    @ExcelProperty(value = "是否有子级")
    private boolean childFlag;
}
