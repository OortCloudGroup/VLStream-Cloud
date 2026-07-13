/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 流程附件视图对象
 */
@Data
public class WfAttachmentVo {
    private static final long serialVersionUID = 1L;

    /**
     * 附件主键
     */
    private Long id;

    /**
     * 流程实例id
     */
    private String procInsId;

    /**
     * 流程节点id
     */
    private String taskId;

    /**
     * 附件链接
     */
    private String attachmentLink;

    /**
     * 是否已经签名（0代表未签名 1代表已签名）
     */
    private String isSignature;
}
