/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.exception.ServiceException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceFirmwareView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDownloadUrl;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUploadGrant;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUploadRequest;
import com.ruoyi.vlstream.test.vlstream.service.DeviceFirmwareService;
import com.ruoyi.vlstream.test.vlstream.service.FirmwareDeploymentService;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwarePackageDownload;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

/** VLS protocol device firmware management APIs. */
@RestController
@RequestMapping("/vlsDeviceFirmware")
@RequiredArgsConstructor
public class VlsDeviceFirmwareController {

	private final DeviceFirmwareService firmwareService;
	private final FirmwareDeploymentService deploymentService;

	@SaCheckPermission("vls:firmware:list")
	@GetMapping("/page")
	public R<Page<DeviceFirmwareView>> page(@RequestParam(defaultValue = "1") long current,
		@RequestParam(defaultValue = "10") long size,
		@RequestParam(required = false) String cameraModel,
		@RequestParam(required = false) String firmwareVersion) {
		return R.data(firmwareService.page(current, size, cameraModel, firmwareVersion));
	}

	@SaCheckPermission("vls:firmware:upload")
	@PostMapping("/upload-grant")
	public R<FirmwareUploadGrant> issueUpload(@RequestBody FirmwareUploadRequest request) {
		return R.data(firmwareService.issueUpload(request));
	}

	@SaCheckPermission("vls:firmware:upload")
	@PostMapping("/{id}/complete")
	public R<DeviceFirmwareView> completeUpload(@PathVariable Long id) {
		return R.data(firmwareService.completeUpload(id));
	}

	@SaCheckPermission("vls:firmware:download")
	@GetMapping("/{id}/download-url")
	public R<FirmwareDownloadUrl> downloadUrl(@PathVariable Long id) {
		return R.data(firmwareService.downloadUrl(id));
	}

	@SaIgnore
	@GetMapping("/ota/{requestId}/{messageId}")
	public void otaPackage(@PathVariable String requestId, @PathVariable String messageId,
		HttpServletResponse response) {
		try {
			FirmwarePackageDownload download = deploymentService.download(requestId, messageId);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(download.getContentType());
			response.setContentLengthLong(download.getFileSize());
			response.setHeader("X-Firmware-SHA256", download.getSha256());
			response.setHeader("Cache-Control", "private, no-store");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
				+ URLEncoder.encode(download.getFileName(), "UTF-8").replace("+", "%20"));
			try (InputStream input = download.getInputStream(); OutputStream output = response.getOutputStream()) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = input.read(buffer)) != -1) {
					output.write(buffer, 0, read);
				}
				output.flush();
			}
		} catch (ServiceException exception) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} catch (Exception exception) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@SaCheckPermission("vls:firmware:remove")
	@DeleteMapping("/{id}")
	public R<Void> delete(@PathVariable Long id) {
		firmwareService.delete(id);
		return R.success();
	}
}
