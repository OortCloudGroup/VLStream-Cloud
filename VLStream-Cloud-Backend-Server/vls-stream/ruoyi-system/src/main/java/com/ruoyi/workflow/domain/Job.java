/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;

import lombok.Data;

import java.util.List;

/**
 * 循环定时信息参数设定
 */
@Data
public class Job {
    /**
     * 定时任务开始时间
     */
    private String start;
    /**
     * 定时任务结束时间
     */
    private String end;
    /**
     * 发送间隔，隔天、每周时生效
     */
    private int interval;
    /**
     * 触发时间（每天、隔天为时分秒；每周为星期；每月为日期）
     */
    private List<String> run;
    /**
     * 定时类型：1每天,2隔天,3每周,4每月
     */
    private int types;
    /**
     * 每周、每月类型触发时的具体时分秒（格式为 HHmmss, 例如 170633表示17:06:33）
     */
    private int trgTime;
}
