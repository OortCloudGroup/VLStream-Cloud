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
 * workflowformobject wf_form
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
     * formprimary key
     */
    @TableId(value = "form_id")
    private String formId;

    /**
     * id
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;
    /**
     * id
     */
    private String categoryId;

    /**
     * form
     */
    private String formName;

    /**
     * form
     */
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
     * 0workflow 1work order
     */
    private String type;
}
