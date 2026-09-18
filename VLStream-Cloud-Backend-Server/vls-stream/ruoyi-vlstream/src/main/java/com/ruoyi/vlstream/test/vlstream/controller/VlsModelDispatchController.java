/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import com.ruoyi.vlstream.test.vlstream.service.ModelDispatchTaskService;
import com.ruoyi.vlstream.test.vlstream.service.ModelDownloadSignatureService;
import com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceModelCandidate;
import com.ruoyi.vlstream.test.vlstream.service.ModelDispatchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Model dispatch status and signed hardware download endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/vlsModelDispatch")
@Tag(name = "模型下发任务", description = "模型下发状态与硬件下载接口")
public class VlsModelDispatchController extends BladeController {

    @Resource
    private VlsAlgorithmModelMapper modelMapper;

    @Resource
    private ModelDispatchService dispatchService;

    @GetMapping("/models")
    @Operation(summary = "查询自主训练模型下发候选")
    public R<IPage<DeviceModelCandidate>> models(
        @RequestParam(defaultValue = "1") long current,
        @RequestParam(defaultValue = "12") long size,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String category) {
        return R.data(modelMapper.selectDeviceCandidates(new Page<>(Math.max(1, current), Math.max(1, Math.min(48, size))),
            StringUtils.trimToEmpty(keyword), StringUtils.trimToEmpty(category)));
    }

    @PostMapping("/models/{modelId}/dispatch")
    @Operation(summary = "向当前设备下发选定自主训练模型版本")
    public R<String> dispatchModel(@PathVariable Long modelId, @RequestParam String deviceId) {
        return R.data(dispatchService.dispatchModel(modelId, deviceId));
    }

	@Resource
	private ModelDispatchTaskService taskService;

	@Resource
	private ModelDownloadSignatureService signatureService;

	@Resource
	private RemoteModelArtifactService artifactService;

	@GetMapping("/task/{requestId}")
	@Operation(summary = "查询模型下发任务")
	public R<ModelDispatchTask> task(@PathVariable String requestId) {
		ModelDispatchTask task = taskService.getByRequestId(requestId);
		return task == null ? R.fail("下发任务不存在") : R.data(task);
	}

	@GetMapping("/tasks")
	@Operation(summary = "查询最近模型下发任务")
	public R<List<ModelDispatchTask>> tasks(
		@RequestParam(required = false) String deviceId,
		@RequestParam(required = false) String status,
		@RequestParam(defaultValue = "50") Integer limit) {
		return R.data(taskService.listRecent(deviceId, status, limit == null ? 50 : limit));
	}

	/**
	 * Hardware uses the HMAC query parameters in the MQTT payload; no platform login token is required.
	 */
	@SaIgnore
	@GetMapping("/public/{requestId}/download")
	@Operation(summary = "硬件下载模型")
	public void download(@PathVariable String requestId,
		@RequestParam long expires,
		@RequestParam String signature,
		HttpServletResponse response) {
		ModelDispatchTask task = taskService.getByRequestId(requestId);
		if (task == null) {
			writeError(response, HttpServletResponse.SC_NOT_FOUND, "Dispatch task not found");
			return;
		}
		if (task.getDownloadExpiresAt() == null || task.getDownloadExpiresAt() != expires
			|| !signatureService.verify(requestId, expires, signature)) {
			writeError(response, HttpServletResponse.SC_FORBIDDEN, "Download link is invalid or expired");
			return;
		}
		if (StringUtils.isBlank(task.getRemotePath())) {
			writeError(response, HttpServletResponse.SC_NOT_FOUND, "Model artifact path is empty");
			return;
		}

		try {
			taskService.markDownloadStarted(requestId);
			String encodedFileName = URLEncoder.encode(task.getFileName(), "UTF-8").replace("+", "%20");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
			response.setHeader("Content-Length", String.valueOf(task.getFileSize()));
			response.setHeader("ETag", "\"" + task.getSha256() + "\"");
			response.setHeader("X-Model-SHA256", task.getSha256());
			response.setHeader("Cache-Control", "private, no-store");
			artifactService.stream(task.getRemotePath(), response.getOutputStream());
			taskService.markDownloadCompleted(requestId);
		} catch (Exception ex) {
			log.error("Hardware model download failed: requestId={}", requestId, ex);
			taskService.markFailed(requestId, "Download failed: " + ex.getMessage());
			if (!response.isCommitted()) {
				writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Model download failed");
			}
		}
	}

	@SaIgnore
	@GetMapping("/public/{requestId}/classes")
	@Operation(summary = "硬件下载模型对应的类别 YAML")
	public void downloadClasses(@PathVariable String requestId,
		@RequestParam long expires, @RequestParam String signature,
		HttpServletResponse response) {
		ModelDispatchTask task = taskService.getByRequestId(requestId);
		if (task == null) {
			writeError(response, HttpServletResponse.SC_NOT_FOUND, "Dispatch task not found");
			return;
		}
		if (task.getDownloadExpiresAt() == null || task.getDownloadExpiresAt() != expires
			|| !signatureService.verify(requestId + ":classes", expires, signature)) {
			writeError(response, HttpServletResponse.SC_FORBIDDEN, "Download link is invalid or expired");
			return;
		}
		if (StringUtils.isBlank(task.getClassFileContent())) {
			writeError(response, HttpServletResponse.SC_NOT_FOUND, "Class file not available for this task");
			return;
		}
		try {
			byte[] bytes = task.getClassFileContent().getBytes(StandardCharsets.UTF_8);
			String name = URLEncoder.encode(task.getClassFileName(), "UTF-8").replace("+", "%20");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/yaml;charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + name);
			response.setContentLengthLong(bytes.length);
			response.setHeader("ETag", "\"" + task.getClassFileSha256() + "\"");
			response.setHeader("X-Class-File-SHA256", task.getClassFileSha256());
			response.setHeader("Cache-Control", "private, no-store");
			response.getOutputStream().write(bytes);
		} catch (Exception ex) {
			log.error("Hardware class file download failed: requestId={}", requestId, ex);
			// A transport retry must not turn an already deployed task into a failure.
			writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Class file download failed");
		}
	}

	private void writeError(HttpServletResponse response, int status, String message) {
		try {
			if (!response.isCommitted()) {
				response.reset();
				response.setStatus(status);
				response.setContentType("text/plain;charset=UTF-8");
				response.getWriter().write(message);
			}
		} catch (Exception ex) {
			log.debug("Failed to write model download error response", ex);
		}
	}
}
