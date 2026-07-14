/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程表单对象 wf_form
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form")
public class WfForm extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 表单主键
     */
    @TableId(value = "form_id")
    private String formId;

    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 分类id
     */
    private String categoryId;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单内容
     */
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
     * 0流程 1工单
     */
    private String type;
}
