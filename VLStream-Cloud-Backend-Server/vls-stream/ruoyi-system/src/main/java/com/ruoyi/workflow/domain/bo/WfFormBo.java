/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
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
 * 流程表单业务对象
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormBo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 表单主键
     */
    @NotNull(message = "表单ID不能为空", groups = {EditGroup.class})
    private String formId;

    /**
     * 表单名称
     */
    @NotBlank(message = "表单名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String formName;

    /**
     * 表单内容
     */
    @NotBlank(message = "表单内容不能为空", groups = {AddGroup.class, EditGroup.class})
    private String content;

    /**
     * 组件标志（0代表表单 1代表组件）
     */
    private String isFormComponents;
    /**
     * 表单类型（0流式布局 1签批卡片布局）
     */
    private Integer formType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 表单分类id
     */
    @NotBlank(message = "所属分类不能为空", groups = {AddGroup.class, EditGroup.class})
    private String categoryId;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 应用表单
     */
    private Boolean wfFormApp=false;
    /**
     * 综合表单
     */
    private Boolean wfFormSynthesis=false;
    /**
     * 0流程 1工单
     */
    private String type;
    /**
     * 模型主键
     */
    private String modelId;
    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;
}
