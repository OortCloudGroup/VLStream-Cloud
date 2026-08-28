/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * model service, Process training and already model .
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
	 * trainingtask ID task Generate model .
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
	 * model ID already model , in successfully after .
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
	 * before model , .
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
	 * trainingtask record .
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
	 * model in already .
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
	 * Validate whether already Generate .
	 */
	private String requirePath(String downloadPath, String type) throws FileNotFoundException {
		if (StringUtils.isBlank(downloadPath)) {
			throw new FileNotFoundException("Model file path is empty for type: " + type);
		}
		return downloadPath.trim();
	}

	/**
	 * SSH HTTP .
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
	 * Shell parameter , in null / empty .
	 */
	private String quoteShellArgument(String value) {
		return "'" + value.replace("'", "'\\\"'\\\"'") + "'";
	}

	/**
	 * in successfully after model .
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
