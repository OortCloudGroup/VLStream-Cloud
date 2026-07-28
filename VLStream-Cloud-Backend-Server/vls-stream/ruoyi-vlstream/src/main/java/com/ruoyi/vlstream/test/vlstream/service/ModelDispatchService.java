/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates durable per-device tasks and publishes short-lived model download instructions.
 */
@Slf4j
@Service
public class ModelDispatchService {

	@Resource
	private IVlsAlgorithmTrainingService trainingService;

	@Resource
	private VlsDeviceInfoMapper deviceInfoMapper;

	@Resource
	private RemoteModelArtifactService artifactService;

	@Resource
	private ModelDispatchTaskService taskService;

	@Resource
	private ModelDispatchMqttService mqttService;

	@Resource
	private ModelDownloadSignatureService signatureService;

	@Resource
	private VlsModelDispatchProperties dispatchProperties;

	public boolean dispatch(Long algorithmId, String deviceIds, String modelType) {
		if (algorithmId == null || StringUtils.isBlank(deviceIds)) {
			throw new ServiceException("算法ID和设备ID不能为空");
		}

		String normalizedType;
		PreparedArtifact preparedArtifact;
		try {
			normalizedType = artifactService.normalizeType(modelType);
		} catch (IllegalArgumentException ex) {
			throw new ServiceException("不支持的模型格式：" + StringUtils.defaultString(modelType), 400);
		}
		preparedArtifact = prepareLatestArtifact(algorithmId, normalizedType);
		validateConfiguration();

		boolean allSucceeded = true;
		int publishedCount = 0;
		List<String> failures = new ArrayList<String>();
		for (String rowIdText : deviceIds.split(",")) {
			if (StringUtils.isBlank(rowIdText)) {
				continue;
			}
			try {
				Long rowId = Long.valueOf(rowIdText.trim());
				DeviceInfo device = deviceInfoMapper.selectById(rowId);
				if (device == null || StringUtils.isBlank(device.getDeviceId())) {
					throw new IllegalArgumentException("Device does not exist or device number is empty: " + rowId);
				}
				dispatchToDevice(algorithmId, preparedArtifact.training, normalizedType,
					preparedArtifact.remotePath, preparedArtifact.metadata, device);
				publishedCount++;
			} catch (Exception ex) {
				allSucceeded = false;
				failures.add("设备记录 " + rowIdText.trim() + "：" + rootMessage(ex));
				log.error("Model dispatch failed for device row: {}", rowIdText, ex);
			}
		}
		if (!allSucceeded) {
			String detail = StringUtils.join(failures, "；");
			if (publishedCount > 0) {
				throw new ServiceException("部分设备模型下发失败：" + detail);
			}
			throw new ServiceException("模型下发失败：" + detail);
		}
		return allSucceeded && publishedCount > 0;
	}

	private void dispatchToDevice(Long algorithmId, AlgorithmTraining training, String modelType,
								  String remotePath,
								  RemoteModelArtifactService.ArtifactMetadata metadata,
								  DeviceInfo device) {
		String requestId = UUID.randomUUID().toString();
		String mqttMessageId = UUID.randomUUID().toString();
		long ttl = dispatchProperties.getDownloadUrlTtlSeconds() == null
			? 1800L : Math.max(60L, dispatchProperties.getDownloadUrlTtlSeconds());
		long expiresAt = System.currentTimeMillis() / 1000L + ttl;
		String signature = signatureService.sign(requestId, expiresAt);
		String downloadUrl = buildDownloadUrl(requestId, expiresAt, signature);
		String topic = VlsMqttProtocol.deviceBusTopic(device.getDeviceId());

		ModelDispatchTask task = new ModelDispatchTask();
		task.setRequestId(requestId);
		task.setMqttMessageId(mqttMessageId);
		task.setDeviceRowId(device.getId());
		task.setDeviceId(device.getDeviceId());
		task.setAlgorithmId(algorithmId);
		task.setTrainingId(training.getId());
		task.setModelType(modelType);
		task.setRemotePath(remotePath);
		task.setFileName(metadata.getFileName());
		task.setFileSize(metadata.getFileSize());
		task.setSha256(metadata.getSha256());
		task.setDispatchStatus("CREATED");
		task.setMqttTopic(topic);
		task.setDownloadExpiresAt(expiresAt);
		taskService.create(task);

		Map<String, Object> modelPayload = new LinkedHashMap<String, Object>();
		modelPayload.put("requestId", requestId);
		modelPayload.put("algorithmId", String.valueOf(algorithmId));
		modelPayload.put("trainingId", String.valueOf(training.getId()));
		modelPayload.put("modelType", modelType);
		modelPayload.put("modelUrl", downloadUrl);
		modelPayload.put("fileName", metadata.getFileName());
		modelPayload.put("fileSize", metadata.getFileSize());
		modelPayload.put("sha256", metadata.getSha256());
		modelPayload.put("expiresAt",
			DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(expiresAt)));
		modelPayload.put("rollbackEnable", Boolean.TRUE);

		Map<String, Object> envelope = new LinkedHashMap<String, Object>();
		envelope.put("protocolVersion", VlsMqttProtocol.VERSION);
		envelope.put("messageId", mqttMessageId);
		envelope.put("deviceId", device.getDeviceId());
		envelope.put("sentAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		envelope.put("msgDir", VlsMqttProtocol.PLATFORM_TO_DEVICE);
		envelope.put("mainBizType", VlsMqttProtocol.AI_BIZ);
		envelope.put("subBizType", VlsMqttProtocol.MODEL_DEPLOY);
		envelope.put("payload", modelPayload);
		envelope.put("extend", new LinkedHashMap<String, Object>());

		try {
			mqttService.publish(topic, envelope);
			taskService.markPublished(requestId, topic);
			DeviceInfo update = new DeviceInfo();
			update.setId(device.getId());
			update.setAlgorithmId(String.valueOf(algorithmId));
			if (deviceInfoMapper.updateById(update) <= 0) {
				log.warn("MQTT published but device algorithm relation was not updated: requestId={}", requestId);
			}
		} catch (RuntimeException ex) {
			taskService.markFailed(requestId, ex.getMessage());
			throw ex;
		}
	}

	private PreparedArtifact prepareLatestArtifact(Long algorithmId, String modelType) {
		List<AlgorithmTraining> candidates = trainingService.list(
			Wrappers.<AlgorithmTraining>lambdaQuery()
				.eq(AlgorithmTraining::getAlgorithmId, algorithmId)
				.eq(AlgorithmTraining::getTrainStatus, AlgorithmTrainingStatusEnum.completed)
				.orderByDesc(AlgorithmTraining::getUpdateTime)
				.orderByDesc(AlgorithmTraining::getId)
				.last("limit 20"));
		if (candidates == null || candidates.isEmpty()) {
			throw new ServiceException("算法 " + algorithmId
				+ " 没有已完成的训练任务，无法下发 " + modelType.toUpperCase() + " 模型");
		}
		String lastFailure = null;
		for (AlgorithmTraining candidate : candidates) {
			try {
				String remotePath = artifactService.resolvePath(candidate, modelType);
				RemoteModelArtifactService.ArtifactMetadata metadata = artifactService.inspect(remotePath);
				return new PreparedArtifact(candidate, remotePath, metadata);
			} catch (Exception ex) {
				lastFailure = rootMessage(ex);
				log.warn("Skip unavailable training artifact: trainingId={}, modelType={}, reason={}",
					candidate.getId(), modelType, ex.getMessage());
			}
		}
		throw new ServiceException("算法 " + algorithmId + " 没有可用的 "
			+ modelType.toUpperCase() + " 模型产物"
			+ (StringUtils.isBlank(lastFailure) ? "" : "：" + lastFailure));
	}

	private static final class PreparedArtifact {
		private final AlgorithmTraining training;
		private final String remotePath;
		private final RemoteModelArtifactService.ArtifactMetadata metadata;

		private PreparedArtifact(AlgorithmTraining training, String remotePath,
								 RemoteModelArtifactService.ArtifactMetadata metadata) {
			this.training = training;
			this.remotePath = remotePath;
			this.metadata = metadata;
		}
	}

	private void validateConfiguration() {
		if (StringUtils.isBlank(dispatchProperties.getPublicBaseUrl())) {
			throw new ServiceException("未配置硬件可访问的模型下载地址 VLSTREAM_MODEL_PUBLIC_BASE_URL");
		}
		if (StringUtils.isBlank(dispatchProperties.getSigningSecret())) {
			throw new ServiceException("未配置模型下载签名密钥 VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET");
		}
	}

	private String rootMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return StringUtils.defaultIfBlank(current.getMessage(), current.getClass().getSimpleName());
	}

	private String buildDownloadUrl(String requestId, long expiresAt, String signature) {
		String baseUrl = StringUtils.stripEnd(dispatchProperties.getPublicBaseUrl().trim(), "/");
		return UriComponentsBuilder.fromHttpUrl(baseUrl)
			.path("/vlsModelDispatch/public/")
			.path(requestId)
			.path("/download")
			.queryParam("expires", expiresAt)
			.queryParam("signature", signature)
			.build()
			.encode()
			.toUriString();
	}
}
