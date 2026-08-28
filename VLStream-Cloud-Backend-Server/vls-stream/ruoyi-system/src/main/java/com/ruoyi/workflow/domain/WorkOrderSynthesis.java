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
 * work orderworkflow object workorder_synthesis
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workorder_synthesis")
public class WorkOrderSynthesis extends TreeEntity<WorkOrderSynthesis> {

    private static final long serialVersionUID=1L;

    /**
     * primary key ID
     */
    @TableId(value = "synthesis_id")
    private String synthesisId;
    /**
     *
     */
    private String categoryName;
    /**
     * workflow
     */
    private String description;
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

}
