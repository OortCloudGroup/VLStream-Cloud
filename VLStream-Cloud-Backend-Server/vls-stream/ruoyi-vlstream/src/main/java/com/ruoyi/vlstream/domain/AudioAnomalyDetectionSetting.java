/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Audio anomaly-detection settings persisted per device. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_audio_anomaly_detection_setting")
public class AudioAnomalyDetectionSetting extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;
    private Integer audioInputAnomalyEnabled;
    private Integer soundRiseEnabled;
    private Integer soundRiseSensitivity;
    private Integer soundIntensityThreshold;
    private Integer soundDropEnabled;
    private Integer soundDropSensitivity;
    private String remark;
}
