/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 应用工单分类视图对象 workorder_app
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Data
@ExcelIgnoreUnannotated
public class WorkOrderAppVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private String appId;

    /**
     * 应用名称
     */
    @ExcelProperty(value = "应用名称")
    private String applicationName;

    /**
     * 应用ID
     */
    @ExcelProperty(value = "应用ID")
    private String applicationId;

    /**
     * 应用密钥
     */
    @ExcelProperty(value = "应用密钥")
    private String applicationSecret;
    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;

    /**
     * 图标地址
     */
    private String images;
    /**
     * 应用包名
     */
    @ExcelProperty(value = "应用包名")
    private String appPackage;

    /**
     * 工单表单分类ID，用于新建模型时自动创建表单
     */
    private String categoryId;
}
