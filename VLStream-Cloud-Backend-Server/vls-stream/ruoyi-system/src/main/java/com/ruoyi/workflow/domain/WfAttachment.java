/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * workflow object wf_attachment
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_attachment")
public class WfAttachment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    @TableId(value = "id")
    private Long id;

    /**
     * id
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;

    /**
     * workflow instanceid
     */
    @TableField("proc_ins_id")
    private String procInsId;

    /**
     * workflownodeid
     */
    @TableField("task_id")
    private String taskId;

    /**
     *
     */
    @TableField("attachment_link")
    private String attachmentLink;

    /**
     * Delete (0represents in 2represents Delete )
     */
    @TableLogic()
    @TableField("del_flag")
    private String delFlag;

    /**
     * whether already (0represents not 1represents already )
     */
    @TableField("is_signature")
    private String isSignature;
}
