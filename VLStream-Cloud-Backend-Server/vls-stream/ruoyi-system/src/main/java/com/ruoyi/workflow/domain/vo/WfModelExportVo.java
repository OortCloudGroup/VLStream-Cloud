/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * workflowmodelobjectExport VO
 *
 * @author konbai
 */
@Data
@NoArgsConstructor
public class WfModelExportVo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * modelID
     */
    @ExcelProperty(value = "模型ID")
    private String modelId;
    /**
     * modelKey
     */
    @ExcelProperty(value = "模型Key")
    private String modelKey;
    /**
     * model
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;
    /**
     *
     */
    @ExcelProperty(value = "分类编码")
    private String category;
    /**
     * workflow
     */
    @ExcelProperty(value = "流程分类")
    private String categoryName;
    /**
     * model
     */
    @ExcelProperty(value = "模型版本")
    private Integer version;
    /**
     * model
     */
    @ExcelProperty(value = "模型描述")
    private String description;
    /**
     * create time
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
