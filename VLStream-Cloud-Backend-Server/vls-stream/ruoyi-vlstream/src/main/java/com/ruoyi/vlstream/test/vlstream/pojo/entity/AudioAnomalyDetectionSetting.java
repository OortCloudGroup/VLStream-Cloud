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
 * 音频异常侦测设置表 实体类
 */
@Data
@TableName("vls_audio_anomaly_detection_setting")
@Schema(description = "AudioAnomalyDetectionSetting对象")
@EqualsAndHashCode(callSuper = true)
public class AudioAnomalyDetectionSetting extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备主键ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "音频输入异常:0否1是")
	private Integer audioInputAnomalyEnabled;

	@Schema(description = "声强陡升:0否1是")
	private Integer soundRiseEnabled;

	@Schema(description = "声强陡升灵敏度")
	private Integer soundRiseSensitivity;

	@Schema(description = "声音强度阈值")
	private Integer soundIntensityThreshold;

	@Schema(description = "声强陡降:0否1是")
	private Integer soundDropEnabled;

	@Schema(description = "声强陡降灵敏度")
	private Integer soundDropSensitivity;

	@Schema(description = "备注")
	private String remark;
}
