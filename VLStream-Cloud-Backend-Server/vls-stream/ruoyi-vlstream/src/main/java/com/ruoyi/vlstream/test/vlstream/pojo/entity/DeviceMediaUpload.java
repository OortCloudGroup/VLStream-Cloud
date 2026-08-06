/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * One pre-signed media upload issued to a field device.
 */
@Data
@TableName("vls_device_media_upload")
public class DeviceMediaUpload {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private String mediaId;
	private String deviceId;
	private String ossConfigKey;
	private String objectKey;
	private String fileName;
	private String contentType;
	private Long fileSize;
	private String sha256;
	private String uploadStatus;
	private Date expiresAt;
	private String boundEventMessageId;
	private Date createTime;
	private Date updateTime;
}
