/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.model;

import lombok.Data;

/**
 * Process configurationmodel
 * approvalnode and Process
 */
@Data
public class TimeoutHandler {
    /**
     * Process ( after Generate , Generate eventID)
     * before need to field
     */
    private String handlerId;

    /**
     * ( value )
     */
    private int triggerTime;

    /**
     * : 1- , 2- , 3- 4-
     */
    private int triggerTimeUnit;

    /**
     * : 1- notification, 2-
     */
    private int triggerType;

    /**
     * notificationobjectID (triggerType=1 , user ID)
     */
    private String notificationUserId;

    /**
     * (triggerType=1 , is empty node priority)
     */
    private Integer priority;

    /**
     * ID (triggerType=1 , is empty node data)
     */
    private String data;
}
