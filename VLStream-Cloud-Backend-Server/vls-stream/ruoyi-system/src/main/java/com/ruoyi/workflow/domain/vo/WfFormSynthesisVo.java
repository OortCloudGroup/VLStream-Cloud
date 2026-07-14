/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 表单分类视图对象 wf_form_synthesis
 *
 * @author 雷超群  工单 通用
 * @date 2024-12-25
 */
@Data
@ExcelIgnoreUnannotated
public class WfFormSynthesisVo {

    private static final long serialVersionUID = 1L;

    /**
     * 表单分类id
     */
    @ExcelProperty(value = "表单分类id")
    private String categoryId;

    /**
     * 租户ID
     */
    @ExcelProperty(value = "租户ID")
    private String tenantId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * 分类父id
     */
    @ExcelProperty(value = "分类父id")
    private String parentId;

    /**
     * 表单分类名称
     */
    @ExcelProperty(value = "表单分类名称")
    private String categoryName;

    /**
     * 分类编码
     */
    @ExcelProperty(value = "分类编码")
    private String code;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
