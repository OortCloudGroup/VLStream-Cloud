/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Map;

/**
 * 时间策略表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName(value = "vls_time_strategy", autoResultMap = true)
@Schema(description = "VlsTimeStrategyEntity对象")
@EqualsAndHashCode(callSuper = true)
public class TimeStrategy extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 设备ID
	 */
	@Schema(description = "设备ID")
	private String deviceId;
	/**
	 *时间策略配置
	 */
	@Schema(description = "时间策略配置")
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> protectionTime;

}
