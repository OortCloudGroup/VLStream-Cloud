/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;


/**
 * 设备信息表 数据传输对象实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceInfoDTO extends DeviceInfo {
	private static final long serialVersionUID = 1L;

	// 非数据库字段，用于查询时关联显示
	@Schema(description = "算法名称", hidden = true)
	@TableField(exist = false)
	private String algorithmName;

	@Schema(description = "创建人姓名", hidden = true)
	@TableField(exist = false)
	private String createdByName;

	@Schema(description = "实例状态描述", hidden = true)
	@TableField(exist = false)
	private String instanceStatusDesc;

	@Schema(description = "健康状态描述", hidden = true)
	@TableField(exist = false)
	private String healthStatusDesc;

	@Schema(description = "运行时长（分钟）", hidden = true)
	@TableField(exist = false)
	private Long runtimeMinutes;

}
