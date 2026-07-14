/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 流程附件对象 wf_attachment
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
     * 附件主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;

    /**
     * 流程实例id
     */
    @TableField("proc_ins_id")
    private String procInsId;

    /**
     * 流程节点id
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 附件链接
     */
    @TableField("attachment_link")
    private String attachmentLink;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic()
    @TableField("del_flag")
    private String delFlag;

    /**
     * 是否已经签名（0代表未签名 1代表已签名）
     */
    @TableField("is_signature")
    private String isSignature;
}
