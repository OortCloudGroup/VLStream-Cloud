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

import java.util.Date;



/**
 * workflow definition object workflow_definition
 *
 * @author KonBAI
 * @date 2022-01-17
 */
@Data
@ExcelIgnoreUnannotated
public class WfDefinitionVo {

    private static final long serialVersionUID = 1L;

    /**
     * workflow definition ID
     */
    @ExcelProperty(value = "流程定义ID")
    private String definitionId;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程名称")
    private String processName;

    /**
     * workflowKey
     */
    @ExcelProperty(value = "流程Key")
    private String processKey;

    /**
     *
     */
    @ExcelProperty(value = "分类编码")
    private String category;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程分类")
    private String categoryName;

    /**
     *
     */
    @ExcelProperty(value = "版本")
    private Integer version;

    /**
     * formID
     */
    @ExcelProperty(value = "表单ID")
    private String formId;

    /**
     * form
     */
    @ExcelProperty(value = "表单名称")
    private String formName;

    /**
     * ID
     */
    @ExcelProperty(value = "部署ID")
    private String deploymentId;

    /**
     * workflowwhether (true: false: )
     */
    @ExcelProperty(value = "流程是否挂起", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "true=挂起,false=激活")
    private Boolean suspended;

    /**
     *
     */
    @ExcelProperty(value = "部署时间")
    private Date deploymentTime;

    /**
     * id
     */
    private String iconId;
}
