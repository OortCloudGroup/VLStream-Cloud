/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

/**
 * vo
 */
@Data
public class WfAppearanceAllCountVo {
    /**
     * all work order
     */
    private Long allCount;

    /**
     * all work order
     */
    private double allCountCompare;

    /**
     * Process work order
     */
    private Long todoCount;

    /**
     * Process work order
     */
    private double todoCountCompare;

    /**
     * already work order
     */
    private Long finishedCount;

    /**
     * already work order
     */
    private double finishedCountCompare;

    /**
     * already work order
     */
    private Long overtimeCount;

    /**
     * already work order
     */
    private double overtimeCountCompare;
}
