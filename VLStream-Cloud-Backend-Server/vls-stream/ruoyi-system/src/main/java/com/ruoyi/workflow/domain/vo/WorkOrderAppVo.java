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
 * work order object workorder_app
 *
 * @author
 * @date 2025-01-04
 */
@Data
@ExcelIgnoreUnannotated
public class WorkOrderAppVo {

    private static final long serialVersionUID = 1L;

    /**
     * primary key ID
     */
    @ExcelProperty(value = "主键ID")
    private String appId;

    /**
     *
     */
    @ExcelProperty(value = "应用名称")
    private String applicationName;

    /**
     * ID
     */
    @ExcelProperty(value = "应用ID")
    private String applicationId;

    /**
     *
     */
    @ExcelProperty(value = "应用密钥")
    private String applicationSecret;
    /**
     * 0 , 1
     */
    private String appFlag;

    /**
     *
     */
    private String images;
    /**
     *
     */
    @ExcelProperty(value = "应用包名")
    private String appPackage;

    /**
     * work orderform ID, new model form
     */
    private String categoryId;
}
