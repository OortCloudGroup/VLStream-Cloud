/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.CopyGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.workflow.convert.ProcessModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * workflowmodelobject
 *
 * @author KonBAI
 * @createTime 2022/6/21 9:16
 */
@Data
public class WfModelBo {
    /**
     * modelprimary key
     */
    @NotNull(message = "模型主键不能为空", groups = {EditGroup.class})
    private String modelId;
    /**
     * model
     */
    @NotNull(message = "模型名称不能为空", groups = {AddGroup.class, EditGroup.class, CopyGroup.class})
    private String modelName;
    /**
     * modelKey
     */
    @NotNull(message = "模型Key不能为空", groups = {AddGroup.class, EditGroup.class, CopyGroup.class})
    private String modelKey;
    /**
     * workflow
     */
    private String wfCategory;
    /**
     * Query full workflow
     */
    private Boolean wfAppAll = false;
    /**
     * Query full workflow
     */
    private Boolean wfSynthesisAll = false;
    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderAppAll = false;
    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderSynthesisAll = false;
    /**
     * work orderworkflow
     */
    private String WorkOrderCategory;

    /**
     *
     */
    private String description;
    /**
     * form (0 1 )
     */
    private Integer formType;

    /**
     * id
     */
    private String deploymentId;

    /**
     * workflowxml
     */
    private String bpmnXml;
    /**
     * formprimary key
     */
// @NotNull(message = " form can is empty", groups = {CopyGroup.class })
    private String formId;
    /**
     * whether to new
     */
    private Boolean newVersion;
    /**
     * id
     */
    private String iconId;

    /**
     * whether 0 ( ) 1 ( )
     */
    private String showMobile = "0";

    /**
     * model id
     */
    @NotBlank(message = "被复制的流程主键不能为空", groups = {CopyGroup.class})
    private String copyModelId;

    /**
     * whether full Push
     */
    private Boolean notifyAllSteps;

    /**
     * id
     */
    private String tenantId;
    /**
     * id
     */
    private String applicationId;

    /**
     * workflowmodeldata
     */
    private ProcessModel processModel;

//    /**
// * startnode formkey
//     */
//
//    private String formKey;


    /**
     * form , whether need to form
     */
    private String  categoryId;

    /**
     * work order is workflow 0workflow 1work order
     */
    private String type;
}
