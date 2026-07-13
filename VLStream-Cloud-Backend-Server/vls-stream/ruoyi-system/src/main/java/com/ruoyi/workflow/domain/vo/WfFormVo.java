/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 流程分类视图对象
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@ExcelIgnoreUnannotated
public class WfFormVo {

    private static final long serialVersionUID = 1L;

    /**
     * 表单主键
     */
    @ExcelProperty(value = "表单ID")
    private String formId;

    /**
     * 表单名称
     */
    @ExcelProperty(value = "表单名称")
    private String formName;

    /**
     * 表单类型（0流式布局 1签批卡片布局）
     */
    private Integer formType;

    @ExcelProperty(value = "所属分类")
    private String categoryId;

    /**
     * 表单内容
     */
//    @ExcelProperty(value = "表单内容")
    private String content;

    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;
    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
