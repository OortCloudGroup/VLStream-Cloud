/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * task object
 *
 * @author KonBAI
 * @createTime 2022/1/8 19:42
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class WfViewerVo {
    /**
     * Get workflow instance history node ( )
     */
    private Set<String> finishedTaskSet;

    /**
     * already
     */
    private Set<String> finishedSequenceFlowSet;

    /**
     * Get workflow instancecurrent in node ( )
     */
    private Set<String> unfinishedTaskSet;

    /**
     * already
     */
    private Set<String> rejectedTaskSet;
}
