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
 * workflow object wf_copy
 *
 * @author KonBAI
 * @date 2022-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_copy")
public class WfCopy extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * primary key
     */
    @TableId(value = "copy_id")
    private Long copyId;

    /**
     * id
     */
    private String tenantId;
    /**
     *
     */
    private String title;
    /**
     * workflowprimary key
     */
    private String processId;
    /**
     * workflow
     */
    private String processName;
    /**
     * workflow primary key
     */
    private String categoryId;
    /**
     * primary key
     */
    private String deploymentId;
    /**
     * workflow instanceprimary key
     */
    private String instanceId;
    /**
     * taskprimary key
     */
    private String taskId;
    /**
     * userprimary key
     */
    private String userId;
    /**
     * Id
     */
    private String originatorId;
    /**
     *
     */
    private String originatorName;
    /**
     * Delete (0represents in 2represents Delete )
     */
    @TableLogic
    private String delFlag;

}
