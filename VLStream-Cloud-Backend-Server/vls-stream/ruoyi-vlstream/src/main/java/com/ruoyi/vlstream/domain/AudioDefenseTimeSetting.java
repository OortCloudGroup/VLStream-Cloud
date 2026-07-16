/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/** Audio arming schedule persisted as JSON per device. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "vls_audio_defense_time_setting", autoResultMap = true)
public class AudioDefenseTimeSetting extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> protectionTime;

    private String remark;
}
