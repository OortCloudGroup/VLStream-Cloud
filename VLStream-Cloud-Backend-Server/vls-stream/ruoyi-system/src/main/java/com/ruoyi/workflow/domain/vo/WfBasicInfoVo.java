/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class WfBasicInfoVo {
    /**
     * 流程分类
     */
    private String processCategory;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 流程编号
     */
    private String processId;
    /**
     * 提交时间
     */
    private Date submissionTime;
    /**
     * 当前任务定义id
     */
    private String taskDefId;
}
