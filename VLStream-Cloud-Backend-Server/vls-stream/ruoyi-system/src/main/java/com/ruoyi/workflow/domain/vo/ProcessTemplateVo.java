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
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

/**
 * workflowInitialize object process_template
 *
 * @author lcq
 * @date 2025-01-07
 */
@Data
@ExcelIgnoreUnannotated
public class ProcessTemplateVo {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "模板ID")
    private String id;

    /**
     * id
     */
    @ExcelProperty(value = "部署id")
    private String deploymentId;

    /**
     * modelid
     */
    @ExcelProperty(value = "模型id")
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
     * whether 0 ( ) 1 ( )
     */
    @ExcelProperty(value = "手机端是否显示 0", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "显=示")
    private String showMobile;

    /**
     * id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     *
     */
    @ExcelProperty(value = "描述")
    private String description;


}
