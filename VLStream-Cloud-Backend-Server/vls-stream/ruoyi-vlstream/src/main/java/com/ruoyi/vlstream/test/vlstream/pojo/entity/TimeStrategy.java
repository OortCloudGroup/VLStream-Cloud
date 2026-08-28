/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 *
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
	 * deviceID
	 */
	@Schema(description = "设备ID")
	private String deviceId;
	/**
	 * configuration
	 */
	@Schema(description = "时间策略配置")
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> protectionTime;

}
