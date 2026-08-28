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
 * workflow object wf_synthesis
 *
 * @author
 * @date 2025-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_synthesis")
public class WfSynthesis extends TreeEntity<WfSynthesis> {

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
