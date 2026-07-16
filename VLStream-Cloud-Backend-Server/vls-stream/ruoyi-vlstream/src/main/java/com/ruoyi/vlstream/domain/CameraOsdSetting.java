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

/** Camera on-screen-display settings persisted per device. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_camera_osd_setting")
public class CameraOsdSetting extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;
    private Integer showName;
    private Integer showDate;
    private Integer showWeek;
    private String channelName;
    private String timeFormat;
    private String dateFormat;
    private Integer overlay1Enabled;
    private String overlay1Text;
    private Integer overlay2Enabled;
    private String overlay2Text;
    private Integer overlay3Enabled;
    private String overlay3Text;
    private String osdProperty;
    private String osdFont;
    private String osdColor;
    private String alignMode;
    private String remark;
}
