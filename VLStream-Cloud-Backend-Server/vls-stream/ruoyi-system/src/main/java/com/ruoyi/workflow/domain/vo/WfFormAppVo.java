/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

import java.util.Date;

/**
 * form object wf_form_app
 *
 * @author
 * @date 2025-04-26
 */
@Data
@ExcelIgnoreUnannotated
public class WfFormAppVo {

    private static final long serialVersionUID = 1L;

    /**
     * form id
     */
    @ExcelProperty(value = "表单分类id")
    private String categoryId;

    /**
     * ID
     */
    @ExcelProperty(value = "应用ID")
    private String applicationId;

    /**
     *
     */
    @ExcelProperty(value = "应用名称")
    private String applicationName;

    /**
     *
     */
    @ExcelProperty(value = "应用密钥")
    private String applicationSecret;

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

    /**
     * create time
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * Delete (0represents in 1represents Delete )
     */
    @ExcelProperty(value = "删除标志", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=代表存在,1=代表删除")
    private String delFlag;
    /**
     * 0 , 1
     */
    private String appFlag;
    /**
     *
     */
    private String images;
}
