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
 * form object wf_form_synthesis
 *
 * @author work order
 * @date 2024-12-25
 */
@Data
@ExcelIgnoreUnannotated
public class WfFormSynthesisVo {

    private static final long serialVersionUID = 1L;

    /**
     * form id
     */
    @ExcelProperty(value = "表单分类id")
    private String categoryId;

    /**
     * tenant ID
     */
    @ExcelProperty(value = "租户ID")
    private String tenantId;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * id
     */
    @ExcelProperty(value = "分类父id")
    private String parentId;

    /**
     * form
     */
    @ExcelProperty(value = "表单分类名称")
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
