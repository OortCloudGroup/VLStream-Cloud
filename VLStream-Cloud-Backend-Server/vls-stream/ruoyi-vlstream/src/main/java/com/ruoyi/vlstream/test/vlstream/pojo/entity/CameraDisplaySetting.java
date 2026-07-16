/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * 摄像机显示设置表 实体类
 */
@Data
@TableName("vls_camera_display_setting")
@Schema(description = "CameraDisplaySetting对象")
@EqualsAndHashCode(callSuper = true)
public class CameraDisplaySetting extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备主键ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "场景")
	private String scene;

	@Schema(description = "亮度")
	private Integer brightness;

	@Schema(description = "对比度")
	private Integer contrast;

	@Schema(description = "饱和度")
	private Integer saturation;

	@Schema(description = "锐度")
	private Integer sharpness;

	@Schema(description = "曝光模式")
	private String exposureMode;

	@Schema(description = "最大快门限制")
	private String maxShutterLimit;

	@Schema(description = "最小快门限制")
	private String minShutterLimit;

	@Schema(description = "增益限制")
	private Integer gainLimit;

	@Schema(description = "低照度电子快门")
	private String lowLightElectronicShutter;

	@Schema(description = "聚焦模式")
	private String focusMode;

	@Schema(description = "最小聚焦距离")
	private String minFocusDistance;

	@Schema(description = "日夜转换")
	private String dayNightSwitch;

	@Schema(description = "灵敏度")
	private Integer sensitivity;

	@Schema(description = "防补光过曝")
	private String antiFillLightOverExposure;

	@Schema(description = "红外灯模式")
	private String infraredLampMode;

	@Schema(description = "亮度限制")
	private Integer brightnessLimit;

	@Schema(description = "背光补偿")
	private String backlightCompensation;

	@Schema(description = "宽动态")
	private String wideDynamic;

	@Schema(description = "强光抑制")
	private String strongLightSuppression;

	@Schema(description = "白平衡")
	private String whiteBalance;

	@Schema(description = "数字降噪")
	private String digitalNoiseReduction;

	@Schema(description = "降噪等级")
	private Integer noiseReductionLevel;

	@Schema(description = "透雾模式")
	private String defogMode;

	@Schema(description = "电子防抖")
	private String electronicStabilization;

	@Schema(description = "镜像")
	private String mirrorMode;

	@Schema(description = "PAL(50HZ)")
	private String pal50hz;

	@Schema(description = "镜头初始化")
	private String lensInitialization;

	@Schema(description = "变倍限制")
	private Integer zoomLimit;

	@Schema(description = "备注")
	private String remark;
}
