/*
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
 * 表单应用分类视图对象 wf_form_app
 *
 * @author 雷超群
 * @date 2025-04-26
 */
@Data
@ExcelIgnoreUnannotated
public class WfFormAppVo {

    private static final long serialVersionUID = 1L;

    /**
     * 表单分类id
     */
    @ExcelProperty(value = "表单分类id")
    private String categoryId;

    /**
     * 应用ID
     */
    @ExcelProperty(value = "应用ID")
    private String applicationId;

    /**
     * 应用名称
     */
    @ExcelProperty(value = "应用名称")
    private String applicationName;

    /**
     * 应用密钥
     */
    @ExcelProperty(value = "应用密钥")
    private String applicationSecret;

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

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @ExcelProperty(value = "删除标志", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=代表存在,1=代表删除")
    private String delFlag;
    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;
    /**
     * 图标地址
     */
    private String images;
}
