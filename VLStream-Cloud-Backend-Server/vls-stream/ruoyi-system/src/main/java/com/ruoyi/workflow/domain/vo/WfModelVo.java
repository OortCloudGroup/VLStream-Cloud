/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * workflowmodel object
 *
 * @author KonBAI
 * @createTime 2022/6/21 9:16
 */
@Data
public class WfModelVo {
    /**
     * modelID
     */
    private String modelId;
    /**
     * model
     */
    private String modelName;
    /**
     * modelKey
     */
    private String modelKey;
    /**
     *
     */
    private String category;
    /**
     *
     */
    private Integer version;
    /**
     * form
     */
    private Integer formType;
    /**
     * formID
     */
    private String formId;
    /**
     * model
     */
    private String description;
    /**
     * create time
     */
    private Date createTime;
    /**
     * workflowxml
     */
    private String bpmnXml;
    /**
     * form
     */
    private String content;
    /**
     * id
     */
    private String iconId;
    /**
     * whether
     */
    private Integer showMobile;

    /**
     * workflow definition : 1: , 2:
     */
    @ExcelProperty(value = "流程定义状态: 1:激活 , 2:挂起")
    private Boolean suspended;

    /**
     * true to already false to not
     */
    private Boolean deploymentStatus;

    /**
     * workflow definitionid
     */
    private String definitionId;

    /**
     * Query full workflow
     */
    private Boolean wfAppAll;

    /**
     * Query full workflow
     */
    private Boolean wfSynthesisAll;

    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderAppAll;

    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderSynthesisAll;
}
