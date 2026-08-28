/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;

import lombok.Data;

import java.util.List;

/**
 * loop infoparameter
 */
@Data
public class Job {
    /**
     * taskstart
     */
    private String start;
    /**
     * taskfinish
     */
    private String end;
    /**
     * , 、
     */
    private int interval;
    /**
     * ( 、 to ; to ; to )
     */
    private List<String> run;
    /**
     * : 1 ,2 ,3 ,4
     */
    private int types;
    /**
     * 、 ( to HHmmss, 170633 17:06:33)
     */
    private int trgTime;
}
