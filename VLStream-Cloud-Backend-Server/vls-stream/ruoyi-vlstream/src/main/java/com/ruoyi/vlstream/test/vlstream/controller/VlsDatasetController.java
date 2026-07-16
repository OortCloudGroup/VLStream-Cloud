/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import com.ruoyi.vlstream.test.vlstream.service.DatasetService;
import org.springframework.web.bind.annotation.*;

/**
 * 数据集管理控制器
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class VlsDatasetController {

	private final DatasetService datasetService;

	/**
	 * 连接远程服务器
	 */
	@PostMapping("/connect")
	@Operation(summary = "连接远程服务器", description = "连接到远程服务器并验证连接状态")
	public R<String> connectToServer(
		@Parameter(description = "服务器地址") @RequestParam String host,
		@Parameter(description = "用户名") @RequestParam String username,
		@Parameter(description = "密码") @RequestParam String password,
		@Parameter(description = "数据集路径") @RequestParam String path) {

		log.info("连接远程服务器：host={}, username={}, path={}", host, username, path);

		try {
			boolean connected = datasetService.connectToServer(host, username, password, path);
			if (connected) {
				return R.success("服务器连接成功");
			} else {
				return R.fail("服务器连接失败");
			}
		} catch (Exception e) {
			log.error("连接服务器失败：", e);
			return R.fail("连接服务器失败：" + e.getMessage());
		}
	}

	/**
	 * 获取数据集文件列表
	 */
	@GetMapping("/files")
	@Operation(summary = "获取文件列表", description = "获取远程服务器指定路径下的文件列表")
	public R<Object> getDatasetFiles(
		@Parameter(description = "服务器地址") @RequestParam String host,
		@Parameter(description = "数据集路径") @RequestParam String path) {

		log.info("获取数据集文件列表：host={}, path={}", host, path);

		try {
			Object files = datasetService.getDatasetFiles(host, path);
			return R.data(files);
		} catch (Exception e) {
			log.error("获取文件列表失败：", e);
			return R.fail("获取文件列表失败：" + e.getMessage());
		}
	}

	/**
	 * 获取文件内容
	 */
	@GetMapping("/file-content")
	@Operation(summary = "获取文件内容", description = "获取远程服务器指定文件的内容")
	public R<String> getFileContent(
		@Parameter(description = "服务器地址") @RequestParam String host,
		@Parameter(description = "数据集路径") @RequestParam String path,
		@Parameter(description = "文件名") @RequestParam String filename) {

		log.info("获取文件内容：host={}, path={}, filename={}", host, path, filename);

		try {
			String content = datasetService.getFileContent(host, path, filename);
			return R.success(content);
		} catch (Exception e) {
			log.error("获取文件内容失败：", e);
			return R.fail("获取文件内容失败：" + e.getMessage());
		}
	}

	/**
	 * 下载文件
	 */
	@GetMapping("/download")
	@Operation(summary = "下载文件", description = "从远程服务器下载指定文件")
	public void downloadFile(
		@Parameter(description = "服务器地址") @RequestParam String host,
		@Parameter(description = "数据集路径") @RequestParam String path,
		@Parameter(description = "文件名") @RequestParam String filename,
		HttpServletResponse response) {

		log.info("下载文件：host={}, path={}, filename={}", host, path, filename);

		try {
			datasetService.downloadFile(host, path, filename, response);
		} catch (Exception e) {
			log.error("下载文件失败：", e);
			try {
				response.setStatus(500);
				response.getWriter().write("下载文件失败：" + e.getMessage());
			} catch (Exception ex) {
				log.error("写入错误响应失败：", ex);
			}
		}
	}
}
