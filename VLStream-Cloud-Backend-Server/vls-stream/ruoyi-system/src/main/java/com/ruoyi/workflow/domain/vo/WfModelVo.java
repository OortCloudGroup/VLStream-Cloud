/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 流程模型视图对象
 *
 * @author KonBAI
 * @createTime 2022/6/21 9:16
 */
@Data
public class WfModelVo {
    /**
     * 模型ID
     */
    private String modelId;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 模型Key
     */
    private String modelKey;
    /**
     * 分类编码
     */
    private String category;
    /**
     * 版本
     */
    private Integer version;
    /**
     * 表单类型
     */
    private Integer formType;
    /**
     * 表单ID
     */
    private String formId;
    /**
     * 模型描述
     */
    private String description;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 流程xml
     */
    private String bpmnXml;
    /**
     * 表单内容
     */
    private String content;
    /**
     * 图标id
     */
    private String iconId;
    /**
     * 手机端是否显示
     */
    private Integer showMobile;

    /**
     * 流程定义状态: 1:激活 , 2:挂起
     */
    @ExcelProperty(value = "流程定义状态: 1:激活 , 2:挂起")
    private Boolean suspended;

    /**
     * true 为已部署 false 为未部署
     */
    private Boolean deploymentStatus;

    /**
     * 流程定义id
     */
    private String definitionId;

    /**
     * 查询全部应用通用流程
     */
    private Boolean wfAppAll;

    /**
     * 查询全部综合通用流程
     */
    private Boolean wfSynthesisAll;

    /**
     * 查询全部应用工单流程
     */
    private Boolean WorkOrderAppAll;

    /**
     * 查询全部综合工单流程
     */
    private Boolean WorkOrderSynthesisAll;
}
