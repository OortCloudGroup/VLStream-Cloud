/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
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
    private IVlsAlgorithmModelService modelService;

    /** Deploy precisely the saved model version, even if its training task was rerun. */
    public String dispatchModel(Long modelId, String deviceId) {
        AlgorithmModel model = modelService.getById(modelId);
        if (model == null || model.getTrainingId() == null || model.getAlgorithmId() == null) {
            throw new ServiceException("自主训练模型不存在或缺少训练来源");
        }
        if (StringUtils.isBlank(model.getModelPath()) || StringUtils.isBlank(model.getOmModelOutputPath())) {
            throw new ServiceException("该模型尚无可下发的 OM 产物或原始 PT 信息");
        }
        DeviceInfo device = wvpDeviceResolver.resolveOnline(deviceId);
        validateConfiguration();
        AlgorithmTraining snapshot = new AlgorithmTraining();
        snapshot.setId(model.getTrainingId());
        snapshot.setTenantId(model.getTenantId());
        snapshot.setAlgorithmId(model.getAlgorithmId());
        snapshot.setModelOutputPath(model.getModelPath());
        snapshot.setOmModelOutputPath(model.getOmModelOutputPath());
        try {
            String path = artifactService.resolvePath(snapshot, "om");
            RemoteModelArtifactService.ArtifactMetadata metadata = artifactService.inspect(path);
            ModelClassFileService.ClassFile classes = classFileService.prepare(snapshot);
            return dispatchToDevice(model.getAlgorithmId(), snapshot, "om", path, metadata, classes, device);
        } catch (Exception ex) {
            throw new ServiceException("模型下发失败：" + rootMessage(ex));
        }
    }

	@Resource
	private WvpVlStreamDeviceResolver wvpDeviceResolver;

	@Resource
	private RemoteModelArtifactService artifactService;

	@Resource
	private ModelClassFileService classFileService;

	@Resource
	private ModelDispatchTaskService taskService;

	@Resource
	private VlsMqttBusService mqttService;

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
		ModelClassFileService.ClassFile classFile;
		try {
			classFile = classFileService.prepare(preparedArtifact.training);
		} catch (Exception ex) {
			throw new ServiceException("模型对应的类别文件不可用：" + rootMessage(ex));
		}

		boolean allSucceeded = true;
		int publishedCount = 0;
		List<String> failures = new ArrayList<String>();
		for (String deviceIdText : deviceIds.split(",")) {
			if (StringUtils.isBlank(deviceIdText)) {
				continue;
			}
			try {
				String deviceId = deviceIdText.trim();
				DeviceInfo device = wvpDeviceResolver.resolve(deviceId);
				dispatchToDevice(algorithmId, preparedArtifact.training, normalizedType,
					preparedArtifact.remotePath, preparedArtifact.metadata, classFile, device);
				publishedCount++;
			} catch (Exception ex) {
				allSucceeded = false;
				failures.add("设备 " + deviceIdText.trim() + "：" + rootMessage(ex));
				log.error("Model dispatch failed for WVP device: {}", deviceIdText, ex);
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

	private String dispatchToDevice(Long algorithmId, AlgorithmTraining training, String modelType,
								  String remotePath,
								  RemoteModelArtifactService.ArtifactMetadata metadata,
								  ModelClassFileService.ClassFile classFile,
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
		task.setClassFileName(classFile.getFileName());
		task.setClassFileContent(classFile.getContent());
		task.setClassFileSize(classFile.getFileSize());
		task.setClassFileSha256(classFile.getSha256());
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
		String classSignature = signatureService.sign(requestId + ":classes", expiresAt);
		modelPayload.put("classFileUrl", buildDownloadUrl(requestId, expiresAt, classSignature, "/classes"));
		modelPayload.put("classFileName", classFile.getFileName());
		modelPayload.put("classFileSize", classFile.getFileSize());
		modelPayload.put("classFileSha256", classFile.getSha256());
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
			return requestId;
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
		return buildDownloadUrl(requestId, expiresAt, signature, "/download");
	}

	private String buildDownloadUrl(String requestId, long expiresAt, String signature, String resource) {
		String baseUrl = StringUtils.stripEnd(dispatchProperties.getPublicBaseUrl().trim(), "/");
		return UriComponentsBuilder.fromHttpUrl(baseUrl)
			.path("/vlsModelDispatch/public/")
			.path(requestId)
			.path(resource)
			.queryParam("expires", expiresAt)
			.queryParam("signature", signature)
			.build()
			.encode()
			.toUriString();
	}
}
