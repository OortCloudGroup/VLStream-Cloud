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
 * 音频联动方式设置表 实体类
 */
@Data
@TableName("vls_audio_linkage_mode_setting")
@Schema(description = "AudioLinkageModeSetting对象")
@EqualsAndHashCode(callSuper = true)
public class AudioLinkageModeSetting extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备主键ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "常规联动:0否1是")
	private Integer conventionalLinkageEnabled;

	@Schema(description = "邮件联动:0否1是")
	private Integer emailLinkageEnabled;

	@Schema(description = "上传中心:0否1是")
	private Integer uploadCenterLinkageEnabled;

	@Schema(description = "联动报警输出:0否1是")
	private Integer alarmOutputLinkageEnabled;

	@Schema(description = "报警输出通道")
	private String alarmOutputChannel;

	@Schema(description = "录像联动:0否1是")
	private Integer recordLinkageEnabled;

	@Schema(description = "录像通道")
	private String recordChannel;

	@Schema(description = "备注")
	private String remark;
}
