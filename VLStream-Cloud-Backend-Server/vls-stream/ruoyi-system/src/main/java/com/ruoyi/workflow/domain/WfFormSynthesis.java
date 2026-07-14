/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单分类对象 wf_form_category
 *
 * @author 雷超群
 * @date 2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_synthesis")
public class WfFormSynthesis extends TreeEntity<WfFormSynthesis> {

    private static final long serialVersionUID=1L;

    /**
     * 表单分类id
     */
    @TableId(value = "category_id")
    private String categoryId;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 表单分类名称
     */
    private String categoryName;
    /**
     * 分类编码
     */
    private String code;
    /**
     * 备注
     */
    private String remark;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;
    /**
     * 0流程 1工单
     */
    private String type;
}
