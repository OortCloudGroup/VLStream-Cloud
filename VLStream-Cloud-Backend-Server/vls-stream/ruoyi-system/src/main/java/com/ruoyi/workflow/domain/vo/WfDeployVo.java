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

import java.util.Date;


/**
 * workflow object
 *
 * @author KonBAI
 * @date 2022-06-30
 */
@Data
@ExcelIgnoreUnannotated
public class WfDeployVo {

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
     *
     */
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
     * workflow definition : 1: , 2: in
     */
    @ExcelProperty(value = "流程定义状态: 1:激活 , 2:中止")
    private Boolean suspended;

    /**
     *
     */
    @ExcelProperty(value = "部署时间")
    private Date deploymentTime;
}
