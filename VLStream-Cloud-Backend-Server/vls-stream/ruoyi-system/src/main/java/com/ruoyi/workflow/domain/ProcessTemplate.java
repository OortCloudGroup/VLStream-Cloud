/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 * workflowInitialize object process_template
 *
 * @author lcq
 * @date 2025-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("process_template")
public class ProcessTemplate extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * ID
     */
    @TableId(value = "id")
    private String id;
    /**
     * id
     */
    private String deploymentId;
    /**
     * modelid
     */
    private String modelId;
    /**
     * modelKey
     */
    private String modelKey;
    /**
     * model
     */
    private String modelName;
    /**
     * whether 0 ( ) 1 ( )
     */
    private String showMobile;
    /**
     * id
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;
    /**
     *
     */
    private String description;
    /**
     * Delete , 0 not Delete , 1 Delete
     */
    @TableLogic
    private String delFlag;

}
