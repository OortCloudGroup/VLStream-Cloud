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
	private com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess publicAccess;

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
        if (!dispatchAccess.tenant().equals(model.getTenantId())) throw new ServiceException("模型不存在或无权访问");
        if (publicAccess != null) publicAccess.training(model.getTrainingId());
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
            return dispatchToDevice(model.getAlgorithmId(), snapshot, "om", path, metadata, classes, device,
                deploymentFileName(model.getModelName(), "om", metadata.getFileName()));
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

    @Resource
    private ModelDispatchAccess dispatchAccess;
    @Resource
    private ModelDispatchLocks dispatchLocks;

    /** Immutable artifact selection shared by all batch devices and every explicit retry. */
    @lombok.Data
    public static class DeploymentSnapshot {
        private Long algorithmId;
        private Long trainingId;
        private Long modelId;
        private String tenantId;
        private String modelType;
        private String trainType;
        private String remotePath;
        private String fileName;
        private Long fileSize;
        private String sha256;
        private String classFileName;
        private String classContent;
        private Long classFileSize;
        private String classSha256;
    }

    public DeploymentSnapshot prepare(Long algorithmId, Long modelId, String type) {
        validateConfiguration();
        String normalized;
        try { normalized = artifactService.normalizeType(type); }
        catch (IllegalArgumentException ex) { throw new ServiceException("不支持的模型格式"); }
        AlgorithmTraining training;
        String path;
        String fileName = null;
        if (modelId != null) {
            AlgorithmModel model = modelService.getById(modelId);
            if (model == null || model.getTrainingId() == null || model.getAlgorithmId() == null
                || !dispatchAccess.tenant().equals(model.getTenantId())) throw new ServiceException("模型不存在或无权访问");
            if (!"om".equals(normalized) || StringUtils.isAnyBlank(model.getModelPath(), model.getOmModelOutputPath())) {
                throw new ServiceException("自主训练模型必须具备原始 PT 和 OM 产物");
            }
            if (publicAccess != null) publicAccess.training(model.getTrainingId());
            training = new AlgorithmTraining();
            training.setId(model.getTrainingId()); training.setAlgorithmId(model.getAlgorithmId());
            training.setTenantId(model.getTenantId()); training.setModelOutputPath(model.getModelPath());
            training.setOmModelOutputPath(model.getOmModelOutputPath());
            AlgorithmTraining source = trainingService.getById(model.getTrainingId());
            if (source == null || !dispatchAccess.tenant().equals(source.getTenantId())) throw new ServiceException("模型训练来源不存在或无权访问");
            training.setConfigParams(source.getConfigParams());
            try { path = artifactService.resolvePath(training, normalized); }
            catch (java.io.FileNotFoundException ex) { throw new ServiceException("所选模型产物不存在"); }
            fileName = deploymentFileName(model.getModelName(), normalized, "model.om");
        } else {
            PreparedArtifact artifact = prepareLatestArtifact(algorithmId, normalized);
            training = artifact.training; path = artifact.remotePath;
        }
        if (!dispatchAccess.tenant().equals(training.getTenantId())) throw new ServiceException("训练产物不属于当前租户");
        String trainType;
        try { trainType = cn.hutool.json.JSONUtil.parseObj(training.getConfigParams()).getStr("trainType"); }
        catch (RuntimeException ex) { throw new ServiceException("训练类型未确认，不能批量部署"); }
        if (!"detect".equals(trainType)) throw new ServiceException("当前设备部署入口仅支持已确认的 detect 目标检测训练，请先核实训练类型和设备后处理");
        try {
            RemoteModelArtifactService.ArtifactMetadata metadata = artifactService.inspect(path);
            ModelClassFileService.ClassFile classes = classFileService.prepare(training);
            DeploymentSnapshot snapshot = new DeploymentSnapshot();
            snapshot.setAlgorithmId(training.getAlgorithmId()); snapshot.setTrainingId(training.getId());
            snapshot.setModelId(modelId); snapshot.setTenantId(training.getTenantId());
            snapshot.setTrainType(trainType);
            snapshot.setModelType(normalized); snapshot.setRemotePath(path);
            snapshot.setFileName(fileName == null ? metadata.getFileName() : fileName);
            snapshot.setFileSize(metadata.getFileSize()); snapshot.setSha256(metadata.getSha256());
            snapshot.setClassFileName(classes.getFileName()); snapshot.setClassContent(classes.getContent());
            snapshot.setClassFileSize(classes.getFileSize()); snapshot.setClassSha256(classes.getSha256());
            return snapshot;
        } catch (Exception ex) { throw new ServiceException("模型或类别文件不可用：" + rootMessage(ex)); }
    }

    public String dispatchSnapshot(DeploymentSnapshot snapshot, String deviceId, String requestId) {
        if (!dispatchAccess.tenant().equals(snapshot.getTenantId())) throw new ServiceException("无权访问该批次产物");
        if (!"detect".equals(snapshot.getTrainType())) throw new ServiceException("该批次的训练类型不兼容当前板端部署");
        if (snapshot.getModelId() != null) {
            AlgorithmModel current = modelService.getById(snapshot.getModelId());
            if (current == null || !dispatchAccess.tenant().equals(current.getTenantId())) throw new ServiceException("锁定模型已删除或无权访问，不能重试");
        }
        if (publicAccess != null) publicAccess.training(snapshot.getTrainingId());
        return dispatchLocks.run("device:" + snapshot.getTenantId() + ":" + deviceId, () -> {
            DeviceInfo device = dispatchAccess.requireDevice(deviceId, snapshot.getModelType(), true);
            taskService.requireNoActiveTask(deviceId);
            // A retry never silently selects a newer training or mutable file.
            try {
                RemoteModelArtifactService.ArtifactMetadata now = artifactService.inspect(snapshot.getRemotePath());
                if (!snapshot.getSha256().equalsIgnoreCase(now.getSha256()) || !snapshot.getFileSize().equals(now.getFileSize())) {
                    throw new ServiceException("批次锁定的模型产物已变化，不能安全重试，请重新选择模型");
                }
                AlgorithmTraining training = new AlgorithmTraining();
                training.setId(snapshot.getTrainingId()); training.setTenantId(snapshot.getTenantId());
                return dispatchToDevice(snapshot.getAlgorithmId(), training, snapshot.getModelType(), snapshot.getRemotePath(),
                    now, new ModelClassFileService.ClassFile(snapshot.getClassFileName(), snapshot.getClassContent(),
                        snapshot.getClassFileSize(), snapshot.getClassSha256()), device, snapshot.getFileName(), requestId, true);
            } catch (RuntimeException ex) { throw ex; }
            catch (Exception ex) { throw new ServiceException("锁定模型读取失败：" + rootMessage(ex)); }
        });
    }

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
					preparedArtifact.remotePath, preparedArtifact.metadata, classFile, device, preparedArtifact.metadata.getFileName());
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
								  DeviceInfo device, String fileName) {
        return dispatchLocks.run("device:" + dispatchAccess.tenant() + ":" + device.getDeviceId(), () -> {
            DeviceInfo authorized = dispatchAccess.requireDevice(device.getDeviceId(), modelType, false);
            taskService.requireNoActiveTask(device.getDeviceId());
            return dispatchToDevice(algorithmId, training, modelType, remotePath, metadata, classFile,
                authorized, fileName, UUID.randomUUID().toString(), false);
        });
    }

    private String dispatchToDevice(Long algorithmId, AlgorithmTraining training, String modelType,
        String remotePath, RemoteModelArtifactService.ArtifactMetadata metadata,
        ModelClassFileService.ClassFile classFile, DeviceInfo device, String fileName, String requestId, boolean uncertainPublish) {
		String mqttMessageId = UUID.randomUUID().toString();
		long ttl = dispatchProperties.getDownloadUrlTtlSeconds() == null
			? 1800L : Math.max(60L, dispatchProperties.getDownloadUrlTtlSeconds());
		long expiresAt = System.currentTimeMillis() / 1000L + ttl;
		String signature = signatureService.sign(requestId, expiresAt);
		String downloadUrl = buildDownloadUrl(requestId, expiresAt, signature);
		String topic = VlsMqttProtocol.deviceBusTopic(device.getDeviceId());

		ModelDispatchTask task = new ModelDispatchTask();
		task.setTenantId(device.getTenantId());
		task.setRequestId(requestId);
		task.setMqttMessageId(mqttMessageId);
		task.setDeviceRowId(device.getId());
		task.setDeviceId(device.getDeviceId());
		task.setAlgorithmId(algorithmId);
		task.setTrainingId(training.getId());
		task.setModelType(modelType);
		task.setRemotePath(remotePath);
		task.setFileName(fileName);
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
		modelPayload.put("fileName", fileName);
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
            if (uncertainPublish) taskService.markUncertain(requestId, "MQTT 发布结果未确认，请等待回执并核实设备");
            else taskService.markUncertain(requestId, "MQTT 发布结果未确认，请核实设备和下发任务");
			throw ex;
		}
	}

	/** Business-facing download name; archive keys and byte identity remain unchanged. */
	static String deploymentFileName(String name, String modelType, String fallback) {
		String base = StringUtils.defaultIfBlank(name, fallback);
		base = StringUtils.defaultIfBlank(base, "model").trim();
		base = base.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_");
		base = base.replaceAll("(?i)\\.(pt|onnx|om|rknn)$", "").replaceAll("^[. ]+|[. ]+$", "");
		if (base.isEmpty()) base = "model";
		// Keep Unicode business names, with a portable UTF-8 filename length.
		while (base.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 220) {
			base = base.substring(0, base.offsetByCodePoints(base.length(), -1));
		}
		return base + "." + modelType;
	}

	private PreparedArtifact prepareLatestArtifact(Long algorithmId, String modelType) {
		com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AlgorithmTraining> query=new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
		query.eq("algorithm_id",algorithmId).eq("train_status",AlgorithmTrainingStatusEnum.completed.getCode()).orderByDesc("update_time","id").last("limit 20");
		if(publicAccess!=null)publicAccess.filter(query,"vls_algorithm_training.id");
		List<AlgorithmTraining> candidates = trainingService.list(query);
		if (candidates == null || candidates.isEmpty()) {
			throw new ServiceException("算法 " + algorithmId
				+ " 没有已完成的训练任务，无法下发 " + modelType.toUpperCase() + " 模型");
		}
		String lastFailure = null;
		for (AlgorithmTraining candidate : candidates) {
			if(publicAccess!=null)publicAccess.training(candidate.getId());
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
