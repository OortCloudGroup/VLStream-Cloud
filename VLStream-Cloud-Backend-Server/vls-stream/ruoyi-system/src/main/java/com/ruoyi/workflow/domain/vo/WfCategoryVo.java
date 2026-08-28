/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * workflow object flow_category
 *
 * @author KonBAI
 * @date 2022-01-15
 */
@Data
@ExcelIgnoreUnannotated
public class WfCategoryVo {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "分类ID")
    private Long categoryId;

    /**
     *
     */
    @ExcelProperty(value = "分类名称")
    private String categoryName;

    /**
     *
     */
    @ExcelProperty(value = "分类编码")
    private String code;

    /**
     * remark
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
