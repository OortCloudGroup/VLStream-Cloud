/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * form object wf_form_category
 *
 * @author
 * @date 2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_synthesis")
public class WfFormSynthesis extends TreeEntity<WfFormSynthesis> {

    private static final long serialVersionUID=1L;

    /**
     * form id
     */
    @TableId(value = "category_id")
    private String categoryId;
    /**
     * tenant ID
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;
    /**
     * form
     */
    private String categoryName;
    /**
     *
     */
    private String code;
    /**
     * remark
     */
    private String remark;
    /**
     * Delete (0represents in 1represents Delete )
     */
    @TableLogic
    private String delFlag;
    /**
     * 0workflow 1work order
     */
    private String type;
}
