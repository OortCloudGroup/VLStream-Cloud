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
 * workflow object
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@ExcelIgnoreUnannotated
public class WfFormVo {

    private static final long serialVersionUID = 1L;

    /**
     * formprimary key
     */
    @ExcelProperty(value = "表单ID")
    private String formId;

    /**
     * form
     */
    @ExcelProperty(value = "表单名称")
    private String formName;

    /**
     * form (0 1 )
     */
    private Integer formType;

    @ExcelProperty(value = "所属分类")
    private String categoryId;

    /**
     * form
     */
// @ExcelProperty(value = "form ")
    private String content;

    /**
     * 0 , 1
     */
    private String appFlag;
    /**
     * remark
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
