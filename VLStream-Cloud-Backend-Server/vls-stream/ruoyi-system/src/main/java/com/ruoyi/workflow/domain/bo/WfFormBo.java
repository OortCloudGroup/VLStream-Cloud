/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * workflowform object
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormBo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * formprimary key
     */
    @NotNull(message = "表单ID不能为空", groups = {EditGroup.class})
    private String formId;

    /**
     * form
     */
    @NotBlank(message = "表单名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String formName;

    /**
     * form
     */
    @NotBlank(message = "表单内容不能为空", groups = {AddGroup.class, EditGroup.class})
    private String content;

    /**
     * component (0represents form 1represents component)
     */
    private String isFormComponents;
    /**
     * form (0 1 )
     */
    private Integer formType;
    /**
     * remark
     */
    private String remark;
    /**
     * form id
     */
    @NotBlank(message = "所属分类不能为空", groups = {AddGroup.class, EditGroup.class})
    private String categoryId;
    /**
     * id
     */
    private String tenantId;
    /**
     * form
     */
    private Boolean wfFormApp=false;
    /**
     * form
     */
    private Boolean wfFormSynthesis=false;
    /**
     * 0workflow 1work order
     */
    private String type;
    /**
     * modelprimary key
     */
    private String modelId;
    /**
     * 0 , 1
     */
    private String appFlag;
}
