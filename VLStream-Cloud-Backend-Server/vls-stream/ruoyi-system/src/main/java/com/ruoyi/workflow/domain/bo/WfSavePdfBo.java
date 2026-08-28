/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * workflowpdf
 */
@Data
public class WfSavePdfBo {

    /**
     * ID
     */
    private Long id;

    /**
     * taskId
     */
    private String taskId;

    /**
     * workflow instanceId
     */
    private String procInsId;

    /**
     *
     */
    private String attachmentLink;

    /**
     * whether
     */
    private String isSignature;
}
