/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单应用分类对象 wf_form_app
 *
 * @author 雷超群
 * @date 2025-04-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_app")
public class WfFormApp extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 表单分类id
     */
    @TableId(value = "category_id")
    private String categoryId;
    /**
     * 应用ID
     */
    private String applicationId;
    /**
     * 应用名称
     */
    private String applicationName;
    /**
     * 应用密钥
     */
    private String applicationSecret;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 分类父id
     */
    private String parentId;
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
     * 0选择应用，1添加应用
     */
    private String appFlag;
    /**
     * 图标地址
     */
    private String images;
    /**
     * 0流程 1工单
     */
    private String type;
}
