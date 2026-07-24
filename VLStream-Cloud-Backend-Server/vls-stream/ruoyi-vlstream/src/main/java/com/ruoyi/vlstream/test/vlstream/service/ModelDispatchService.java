/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
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

	@Resource
	private VlsMqttProperties mqttProperties;

	public boolean dispatch(Long algorithmId, String deviceIds, String modelType) {
		if (algorithmId == null || StringUtils.isBlank(deviceIds)) {
			return false;
		}

		String normalizedType;
		PreparedArtifact preparedArtifact;
		try {
			normalizedType = artifactService.normalizeType(modelType);
			preparedArtifact = prepareLatestArtifact(algorithmId, normalizedType);
			if (preparedArtifact == null) {
				log.error("No completed training artifact found: algorithmId={}, modelType={}",
					algorithmId, normalizedType);
				return false;
			}
			validateConfiguration();
		} catch (Exception ex) {
			log.error("Cannot prepare model dispatch: algorithmId={}, modelType={}",
				algorithmId, modelType, ex);
			return false;
		}

		boolean allSucceeded = true;
		int publishedCount = 0;
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
				log.error("Model dispatch failed for device row: {}", rowIdText, ex);
			}
		}
		return allSucceeded && publishedCount > 0;
	}

	private void dispatchToDevice(Long algorithmId, AlgorithmTraining training, String modelType,
								  String remotePath,
								  RemoteModelArtifactService.ArtifactMetadata metadata,
								  DeviceInfo device) {
		String requestId = UUID.randomUUID().toString();
		long ttl = dispatchProperties.getDownloadUrlTtlSeconds() == null
			? 1800L : Math.max(60L, dispatchProperties.getDownloadUrlTtlSeconds());
		long expiresAt = System.currentTimeMillis() / 1000L + ttl;
		String signature = signatureService.sign(requestId, expiresAt);
		String downloadUrl = buildDownloadUrl(requestId, expiresAt, signature);
		String topic = StringUtils.defaultIfBlank(
			mqttProperties.getDispatchAlgorithmsTopic(), "oortcloud/dispatchAlgorithms");
		String replyTopic = buildDeviceReplyTopic(device.getDeviceId());

		ModelDispatchTask task = new ModelDispatchTask();
		task.setRequestId(requestId);
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

		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("requestId", requestId);
		payload.put("deviceId", device.getDeviceId());
		payload.put("algorithmId", algorithmId);
		payload.put("trainingId", training.getId());
		payload.put("modelType", modelType);
		payload.put("modelUrl", downloadUrl);
		payload.put("fileName", metadata.getFileName());
		payload.put("fileSize", metadata.getFileSize());
		payload.put("sha256", metadata.getSha256());
		payload.put("expiresAt", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(expiresAt)));
		payload.put("replyTopic", replyTopic);

		try {
			mqttService.publish(topic, payload);
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
		for (AlgorithmTraining candidate : candidates) {
			try {
				String remotePath = artifactService.resolvePath(candidate, modelType);
				RemoteModelArtifactService.ArtifactMetadata metadata = artifactService.inspect(remotePath);
				return new PreparedArtifact(candidate, remotePath, metadata);
			} catch (Exception ex) {
				log.warn("Skip unavailable training artifact: trainingId={}, modelType={}, reason={}",
					candidate.getId(), modelType, ex.getMessage());
			}
		}
		return null;
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
			throw new IllegalStateException("VLSTREAM_MODEL_PUBLIC_BASE_URL is not configured");
		}
		if (StringUtils.isBlank(dispatchProperties.getSigningSecret())) {
			throw new IllegalStateException("VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET is not configured");
		}
		String replyTopic = dispatchProperties.getReplyTopic();
		if (StringUtils.isBlank(replyTopic) || !replyTopic.endsWith("/#")) {
			throw new IllegalStateException(
				"VLSTREAM_MODEL_DISPATCH_REPLY_TOPIC must end with /#");
		}
	}

	private String buildDeviceReplyTopic(String deviceId) {
		String replyTopicFilter = dispatchProperties.getReplyTopic();
		return replyTopicFilter.substring(0, replyTopicFilter.length() - 1) + deviceId;
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
