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

/** Camera image/display settings persisted per device. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_camera_display_setting")
public class CameraDisplaySetting extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;
    private String scene;
    private Integer brightness;
    private Integer contrast;
    private Integer saturation;
    private Integer sharpness;
    private String exposureMode;
    private String maxShutterLimit;
    private String minShutterLimit;
    private Integer gainLimit;
    private String lowLightElectronicShutter;
    private String focusMode;
    private String minFocusDistance;
    private String dayNightSwitch;
    private Integer sensitivity;
    private String antiFillLightOverExposure;
    private String infraredLampMode;
    private Integer brightnessLimit;
    private String backlightCompensation;
    private String wideDynamic;
    private String strongLightSuppression;
    private String whiteBalance;
    private String digitalNoiseReduction;
    private Integer noiseReductionLevel;
    private String defogMode;
    private String electronicStabilization;
    private String mirrorMode;
    private String pal50hz;
    private String lensInitialization;
    private Integer zoomLimit;
    private String remark;
}
