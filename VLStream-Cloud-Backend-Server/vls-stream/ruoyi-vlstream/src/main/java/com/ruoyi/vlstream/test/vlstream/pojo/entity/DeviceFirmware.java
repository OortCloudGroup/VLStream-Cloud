/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Date;

/** VLS protocol device firmware package metadata. */
@Data
@TableName("vls_device_firmware")
@EqualsAndHashCode(callSuper = true)
public class DeviceFirmware extends TenantEntity {
	private String cameraModel;
	private String target;
	private String firmwareVersion;
	private String ossConfigKey;
	private String objectKey;
	private String originalFileName;
	private String contentType;
	private Long fileSize;
	private String sha256;
	private String uploadStatus;
	private Date uploadExpiresAt;
}
