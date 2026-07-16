/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/** Camera event-triggered recording and alarm strategy. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "vls_record_event_strategy", autoResultMap = true)
public class RecordEventStrategy extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private Boolean motionDetectionEnabled;
    private Boolean ptzAlarmReportEnabled;
    private Boolean dynamicAnalysisEnabled;
    private Boolean occlusionAlarmEnabled;
    private String triggerAction;
    private Integer preRecordSeconds;
    private Integer postRecordSeconds;
    private Integer alarmFrequencyMinutes;
    private String alarmLevel;
    private String alarmMethod;
    private String receiverIds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> protectionTime;
}
