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
 * 设备标签关联表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_device_tag_relation")
@Schema(description = "VlsDeviceTagRelationEntity对象")
@EqualsAndHashCode(callSuper = true)
public class DeviceTagRelation extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 设备ID，关联device_info.id
	 */
	@Schema(description = "设备ID，关联device_info.id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;
	/**
	 * 标签ID，关联tag_management.id
	 */
	@Schema(description = "标签ID，关联tag_management.id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long tagId;

}
