/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * workflow object wf_synthesis
 *
 * @author
 * @date 2025-01-04
 */
@Data
@ExcelIgnoreUnannotated
public class WfSynthesisVo {

    private static final long serialVersionUID = 1L;

    /**
     * primary key ID
     */
    @ExcelProperty(value = "主键ID")
    private String synthesisId;

    /**
     * nodeID
     */
    @ExcelProperty(value = "父节点ID")
    private String parentId;

    /**
     *
     */
    @ExcelProperty(value = "分类名称")
    private String categoryName;


}
