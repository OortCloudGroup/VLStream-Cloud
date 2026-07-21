/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Locale;

/**
 * 模型文件下载服务，统一处理训练产物和已发布模型的远程文件读取。
 */
@Slf4j
@Service
public class ModelFileDownloadService {

	@Resource
	private IVlsAlgorithmTrainingService algorithmTrainingService;

	@Resource
	private IVlsAlgorithmModelService algorithmModelService;

	@Resource
	private SSHService sshService;

	@Resource
	private VlsSshProperties sshProperties;

	/**
	 * 按训练任务 ID 下载该任务直接生成的模型文件。
	 */
	public void downloadTrainingModel(Long trainingId, String type, HttpServletResponse response) throws IOException {
		AlgorithmTraining training = algorithmTrainingService.getById(trainingId);
		if (training == null) {
			throw new FileNotFoundException("Training task not found: " + trainingId);
		}

		String downloadPath = resolveTrainingPath(training, normalizeType(type));
		writeRemoteFile(downloadPath, response);
	}

	/**
	 * 按模型表 ID 下载已入库的模型文件，并在成功后增加下载次数。
	 */
	public void downloadModel(Long modelId, String type, HttpServletResponse response) throws IOException {
		AlgorithmModel model = algorithmModelService.getById(modelId);
		if (model == null) {
			throw new FileNotFoundException("Model not found: " + modelId);
		}

		String downloadPath = resolveModelPath(model, normalizeType(type));
		writeRemoteFile(downloadPath, response);
		incrementDownloadCount(modelId);
	}

	/**
	 * 将前端传入的模型格式标准化，并拒绝不支持的格式。
	 */
	private String normalizeType(String type) {
		String normalizedType = StringUtils.defaultIfBlank(type, "pt").trim().toLowerCase(Locale.ROOT);
		if (!"pt".equals(normalizedType) && !"onnx".equals(normalizedType)
			&& !"rknn".equals(normalizedType) && !"int8-rknn".equals(normalizedType)
			&& !"om".equals(normalizedType)) {
			throw new IllegalArgumentException("Unsupported model type: " + type);
		}
		return normalizedType;
	}

	/**
	 * 根据格式选择训练任务自身记录的产物路径。
	 */
	private String resolveTrainingPath(AlgorithmTraining training, String type) throws FileNotFoundException {
		String downloadPath;
		switch (type) {
			case "onnx":
				downloadPath = training.getOnnxModelOutputPath();
				break;
			case "rknn":
				downloadPath = training.getRknnModelOutputPath();
				break;
			case "int8-rknn":
				downloadPath = training.getInt8RknnModelOutputPath();
				break;
			case "om":
				downloadPath = training.getOmModelOutputPath();
				break;
			case "pt":
			default:
				downloadPath = training.getModelOutputPath();
				break;
		}
		return requirePath(downloadPath, type);
	}

	/**
	 * 根据格式选择模型表中已保存的文件路径。
	 */
	private String resolveModelPath(AlgorithmModel model, String type) throws FileNotFoundException {
		String downloadPath;
		switch (type) {
			case "onnx":
				downloadPath = model.getOnnxModelPath();
				break;
			case "rknn":
				downloadPath = model.getRknnModelPath();
				break;
			case "int8-rknn":
				downloadPath = model.getInt8RknnModelOutputPath();
				break;
			case "om":
				downloadPath = model.getOmModelOutputPath();
				break;
			case "pt":
			default:
				downloadPath = model.getModelPath();
				break;
		}
		return requirePath(downloadPath, type);
	}

	/**
	 * 校验对应格式是否已经生成了可下载路径。
	 */
	private String requirePath(String downloadPath, String type) throws FileNotFoundException {
		if (StringUtils.isBlank(downloadPath)) {
			throw new FileNotFoundException("Model file path is empty for type: " + type);
		}
		return downloadPath.trim();
	}

	/**
	 * 通过 SSH 读取远程文件并写入 HTTP 响应。
	 */
	private void writeRemoteFile(String downloadPath, HttpServletResponse response) throws IOException {
		SSHService.SSHExecutionResult result = sshService.executeCommand(
			sshProperties.getHost(),
			sshProperties.getPort(),
			sshProperties.getUsername(),
			sshProperties.getPassword(),
			"base64 -- " + quoteShellArgument(downloadPath)
		);

		if (!result.isSuccess() || StringUtils.isBlank(result.getOutput())) {
			throw new FileNotFoundException("Model file not found: " + downloadPath);
		}

		byte[] fileContent;
		try {
			fileContent = Base64.getDecoder().decode(result.getOutput().replaceAll("\\s+", ""));
		} catch (IllegalArgumentException ex) {
			throw new IOException("Remote model file content is invalid", ex);
		}

		String fileName = downloadPath.substring(downloadPath.lastIndexOf('/') + 1);
		String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
		response.setContentLength(fileContent.length);
		response.getOutputStream().write(fileContent);
		response.getOutputStream().flush();
		log.info("模型文件下载成功: {}", fileName);
	}

	/**
	 * 对远程 Shell 参数做单引号转义，避免路径中的空格或特殊字符改变命令含义。
	 */
	private String quoteShellArgument(String value) {
		return "'" + value.replace("'", "'\\\"'\\\"'") + "'";
	}

	/**
	 * 在文件成功写入响应后增加模型下载次数。
	 */
	private void incrementDownloadCount(Long modelId) {
		try {
			UpdateWrapper<AlgorithmModel> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("id", modelId).setSql("download_count = download_count + 1");
			if (!algorithmModelService.update(new AlgorithmModel(), updateWrapper)) {
				log.warn("Failed to increment download count, modelId={}", modelId);
			}
		} catch (Exception ex) {
			log.warn("Failed to increment download count, modelId={}, error={}", modelId, ex.getMessage());
		}
	}
}
