/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadResponse;
import com.ruoyi.vlstream.test.vlstream.service.DeviceMediaUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * Direct-to-MinIO upload grants and private event image access.
 */
@RestController
@RequestMapping("/vlsDeviceMedia")
@Tag(name = "硬件事件媒体", description = "硬件事件图片预签名上传与私有访问")
public class VlsDeviceMediaController {

	@Resource
	private DeviceMediaUploadService uploadService;

	/**
	 * LAN test endpoint. It is deliberately controlled by
	 * VLSTREAM_DEVICE_MEDIA_ALLOW_UNAUTHENTICATED and must be disabled in production.
	 */
	@SaIgnore
	@PostMapping("/public/upload-url")
	@Operation(summary = "申请事件图片预签名 PUT 地址")
	public R<DeviceMediaUploadResponse> issueUploadUrl(
		@Valid @RequestBody DeviceMediaUploadRequest request) {
		return R.data(uploadService.issueUploadUrl(request));
	}

	@GetMapping("/{mediaId}/view-url")
	@Operation(summary = "获取事件图片短期私有访问地址")
	public R<String> viewUrl(@PathVariable String mediaId,
							 @RequestParam(defaultValue = "300") Integer seconds) {
		return R.data(uploadService.getPrivateViewUrl(
			mediaId, seconds == null ? 300 : seconds));
	}
}
