/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
 * 应用工单分类对象 workorder_app
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workorder_app")
public class WorkOrderApp extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "app_id")
    private String appId;
    /**
     * 应用名称
     */
    private String applicationName;
    /**
     * 应用ID
     */
    private String applicationId;
    /**
     * 应用密钥
     */
    private String applicationSecret;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 删除标记，0表示未删除，1表示删除
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
     * 应用包名
     */
    private String appPackage;
}
