/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * workflow object
 */
@Data
public class WfAttachmentVo {
    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    private Long id;

    /**
     * workflow instanceid
     */
    private String procInsId;

    /**
     * workflownodeid
     */
    private String taskId;

    /**
     *
     */
    private String attachmentLink;

    /**
     * whether already (0represents not 1represents already )
     */
    private String isSignature;
}
