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
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * work order object workorder_app
 *
 * @author
 * @date 2025-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workorder_app")
public class WorkOrderApp extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * primary key ID
     */
    @TableId(value = "app_id")
    private String appId;
    /**
     *
     */
    private String applicationName;
    /**
     * ID
     */
    private String applicationId;
    /**
     *
     */
    private String applicationSecret;
    /**
     * id
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;
    /**
     * Delete , 0 not Delete , 1 Delete
     */
    @TableLogic
    private String delFlag;
    /**
     * 0 , 1
     */
    private String appFlag;

    /**
     *
     */
    private String images;
    /**
     *
     */
    private String appPackage;
}
