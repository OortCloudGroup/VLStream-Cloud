/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.model;

import lombok.Data;

/**
 * 超时处理器配置模型
 * 用于定义审批节点的超时触发条件和处理方式
 */
@Data
public class TimeoutHandler {
    /**
     * 处理器唯一标识（后端自动生成，用于生成边界事件ID）
     * 前端不需要传递此字段
     */
    private String handlerId;

    /**
     * 触发时间（数值）
     */
    private int triggerTime;

    /**
     * 触发时间单位：1-天，2-小时，3-分钟 4-秒
     */
    private int triggerTimeUnit;

    /**
     * 触发类型：1-消息通知，2-自动通过
     */
    private int triggerType;

    /**
     * 通知对象ID（triggerType=1时必需，单个用户ID）
     */
    private String notificationUserId;

    /**
     * 消息优先级（triggerType=1时可选，为空则使用节点的priority）
     */
    private Integer priority;

    /**
     * 消息内容ID（triggerType=1时可选，为空则使用节点的data）
     */
    private String data;
}
