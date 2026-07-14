/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

/**
 * 仪表板统计vo
 */
@Data
public class WfAppearanceAllCountVo {
    /**
     * 所有工单数量
     */
    private Long allCount;

    /**
     * 所有工单数量比较
     */
    private double allCountCompare;

    /**
     * 待处理工单
     */
    private Long todoCount;

    /**
     * 待处理工单数量比较
     */
    private double todoCountCompare;

    /**
     * 已完成工单
     */
    private Long finishedCount;

    /**
     * 已完成工单数量比较
     */
    private double finishedCountCompare;

    /**
     * 已逾期工单
     */
    private Long overtimeCount;

    /**
     * 已逾期工单数量比较
     */
    private double overtimeCountCompare;
}
