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
 * 摄像机OSD设置表 实体类
 */
@Data
@TableName("vls_camera_osd_setting")
@Schema(description = "CameraOsdSetting对象")
@EqualsAndHashCode(callSuper = true)
public class CameraOsdSetting extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备主键ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "显示名称:0否1是")
	private Integer showName;

	@Schema(description = "显示日期:0否1是")
	private Integer showDate;

	@Schema(description = "显示星期:0否1是")
	private Integer showWeek;

	@Schema(description = "通道名称")
	private String channelName;

	@Schema(description = "时间格式")
	private String timeFormat;

	@Schema(description = "日期格式")
	private String dateFormat;

	@Schema(description = "字符叠加1启用:0否1是")
	private Integer overlay1Enabled;

	@Schema(description = "字符叠加1内容")
	private String overlay1Text;

	@Schema(description = "字符叠加2启用:0否1是")
	private Integer overlay2Enabled;

	@Schema(description = "字符叠加2内容")
	private String overlay2Text;

	@Schema(description = "字符叠加3启用:0否1是")
	private Integer overlay3Enabled;

	@Schema(description = "字符叠加3内容")
	private String overlay3Text;

	@Schema(description = "OSD属性")
	private String osdProperty;

	@Schema(description = "OSD字体")
	private String osdFont;

	@Schema(description = "OSD颜色")
	private String osdColor;

	@Schema(description = "对齐方式")
	private String alignMode;

	@Schema(description = "备注")
	private String remark;
}
