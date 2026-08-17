package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.compat.SingleTenant;
import com.ruoyi.vlstream.test.vlstream.enums.FirmwareTarget;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployTaskView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUpgradeCandidate;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwarePackageDownload;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.MqttDeviceDetailView;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceFirmware;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.FirmwareDeployTask;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Evaluates compatible firmware and publishes auditable OTA commands. */
@Service
@RequiredArgsConstructor
public class FirmwareDeploymentService {

	private final MqttDeviceMapper deviceMapper;
	private final DeviceFirmwareService firmwareService;
	private final FirmwareDeployTaskService taskService;
	private final VlsMqttBusService mqttService;
	private final VlsFirmwareProperties properties;

	public MqttDeviceDetailView detail(Long deviceRowId) {
		MqttDevice device = requiredDevice(deviceRowId);
		List<FirmwareUpgradeCandidate> upgrades = new ArrayList<FirmwareUpgradeCandidate>();
		addUpgrade(upgrades, device, FirmwareTarget.APPLICATION, device.getApplicationVersion());
		addUpgrade(upgrades, device, FirmwareTarget.ROOTFS, device.getRootfsVersion());

		boolean hasNewFirmware = !upgrades.isEmpty();
		boolean canUpgrade = hasNewFirmware && Boolean.TRUE.equals(device.getOnline());
		String blockedReason = null;
		if (StringUtils.isBlank(device.getDeviceModel())) {
			blockedReason = "设备尚未上报 deviceModel";
		} else if (!Boolean.TRUE.equals(device.getOnline())) {
			blockedReason = "设备离线，不能创建短期下载地址";
		} else if (!hasKnownCurrentVersion(device)) {
			blockedReason = "设备尚未上报可比较的固件版本";
		} else if (!hasNewFirmware) {
			blockedReason = "当前已是最新兼容版本";
		}
		if (StringUtils.isNotBlank(device.getDeviceModel()) && hasKnownCurrentVersion(device)
			&& Boolean.TRUE.equals(device.getOnline())) {
			canUpgrade = hasNewFirmware;
		}
		return MqttDeviceDetailView.builder()
			.device(device)
			.hasNewFirmware(hasNewFirmware)
			.canUpgrade(canUpgrade)
			.upgradeBlockedReason(blockedReason)
			.availableUpgrades(upgrades)
			.latestTask(taskService.toView(taskService.latestForDevice(deviceRowId)))
			.build();
	}

	public FirmwareDeployTaskView deploy(Long deviceRowId, Long firmwareId) {
		MqttDevice device = requiredDevice(deviceRowId);
		if (!Boolean.TRUE.equals(device.getOnline())) {
			throw new ServiceException("设备离线，无法安全下发带有效期的 OTA 下载地址");
		}
		if (StringUtils.isBlank(device.getDeviceModel())) {
			throw new ServiceException("设备尚未上报 deviceModel，无法匹配固件");
		}
		DeviceFirmware firmware = firmwareService.requiredReady(firmwareId);
		if (!StringUtils.equals(device.getDeviceModel(), firmware.getCameraModel())) {
			throw new ServiceException("固件型号与设备上报型号不一致");
		}
		FirmwareTarget target;
		try {
			target = FirmwareTarget.fromValue(firmware.getTarget());
		} catch (IllegalArgumentException exception) {
			throw new ServiceException("固件升级目标无效");
		}
		String currentVersion = currentVersion(device, target);
		if (!FirmwareVersion.isValid(currentVersion)) {
			throw new ServiceException("设备未上报 " + target.getValue() + " 的有效当前版本");
		}
		if (!FirmwareVersion.isGreater(firmware.getFirmwareVersion(), currentVersion)) {
			throw new ServiceException("目标固件版本必须高于设备当前版本");
		}
		DeviceFirmware latest = firmwareService.findLatestReady(device.getDeviceModel(), target.getValue());
		if (latest == null || !firmware.getId().equals(latest.getId())) {
			throw new ServiceException("所选固件不是该型号和升级目标的最新可用版本，请刷新设备详情");
		}
		if (taskService.hasActiveTask(device.getId(), target.getValue())) {
			throw new ServiceException("该设备的 " + target.getValue() + " OTA 任务仍在处理中");
		}

		String requestId = UUID.randomUUID().toString();
		String messageId = "fw-" + UUID.randomUUID().toString();
		String topic = VlsMqttProtocol.deviceBusTopic(device.getDeviceId());
		int ttlSeconds = firmwareService.otaDownloadTtlSeconds();
		long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
		String packageUrl = platformDownloadUrl(requestId, messageId);

		FirmwareDeployTask task = new FirmwareDeployTask();
		task.setRequestId(requestId);
		task.setMqttMessageId(messageId);
		task.setDeviceRowId(device.getId());
		task.setDeviceId(device.getDeviceId());
		task.setDeviceModel(device.getDeviceModel());
		task.setTarget(target.getValue());
		task.setCurrentVersion(currentVersion);
		task.setTargetVersion(firmware.getFirmwareVersion());
		task.setFirmwareId(firmware.getId());
		task.setFileName(firmware.getOriginalFileName());
		task.setFileSize(firmware.getFileSize());
		task.setSha256(firmware.getSha256());
		task.setRollbackEnable(target.isRollbackEnable());
		task.setRebootAfter(target.isRebootAfter());
		task.setDeployStatus("CREATED");
		task.setMqttTopic(topic);
		task.setDownloadExpiresAt(expiresAt);
		taskService.create(task);

		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("requestId", requestId);
		payload.put("deviceModel", device.getDeviceModel());
		payload.put("target", target.getValue());
		payload.put("version", firmware.getFirmwareVersion());
		payload.put("packageUrl", packageUrl);
		payload.put("urlExpiresAt", Instant.ofEpochSecond(expiresAt).toString());
		payload.put("fileName", firmware.getOriginalFileName());
		payload.put("fileSize", firmware.getFileSize());
		payload.put("sha256", firmware.getSha256());
		payload.put("rollbackEnable", target.isRollbackEnable());
		payload.put("rebootAfter", target.isRebootAfter());

		Map<String, Object> envelope = new LinkedHashMap<String, Object>();
		envelope.put("protocolVersion", VlsMqttProtocol.VERSION);
		envelope.put("messageId", messageId);
		envelope.put("deviceId", device.getDeviceId());
		envelope.put("sentAt", Instant.now().toString());
		envelope.put("msgDir", VlsMqttProtocol.PLATFORM_TO_DEVICE);
		envelope.put("mainBizType", VlsMqttProtocol.DEVICE_BIZ);
		envelope.put("subBizType", VlsMqttProtocol.FIRMWARE_DEPLOY);
		envelope.put("payload", payload);
		envelope.put("extend", new LinkedHashMap<String, Object>());

		try {
			mqttService.publish(topic, envelope);
			taskService.markPublished(requestId);
			task.setDeployStatus("PUBLISHED");
			task.setPublishedAt(new java.util.Date());
			return taskService.toView(task);
		} catch (RuntimeException exception) {
			taskService.markPublishFailed(requestId, exception.getMessage());
			throw new ServiceException("OTA 指令发布失败：" + rootMessage(exception));
		}
	}

	public FirmwarePackageDownload download(String requestId, String messageId) {
		FirmwareDeployTask task = taskService.getByRequestId(requestId);
		if (task == null
			|| !StringUtils.equals(task.getMqttMessageId(), StringUtils.trim(messageId))
			|| task.getDownloadExpiresAt() == null
			|| task.getDownloadExpiresAt() < Instant.now().getEpochSecond()) {
			throw new ServiceException("OTA 下载地址无效或已过期");
		}
		FirmwarePackageDownload download = firmwareService.openOtaPackage(task.getFirmwareId());
		if (download.getFileSize() != task.getFileSize()
			|| !StringUtils.equalsIgnoreCase(download.getSha256(), task.getSha256())) {
			try {
				download.getInputStream().close();
			} catch (Exception ignored) {
				// Preserve the integrity error below.
			}
			throw new ServiceException("OTA 固件元数据与下发任务不一致");
		}
		return download;
	}

	private void addUpgrade(List<FirmwareUpgradeCandidate> upgrades, MqttDevice device,
		FirmwareTarget target, String currentVersion) {
		if (StringUtils.isBlank(device.getDeviceModel()) || !FirmwareVersion.isValid(currentVersion)) {
			return;
		}
		DeviceFirmware latest = firmwareService.findLatestReady(device.getDeviceModel(), target.getValue());
		if (latest == null || !FirmwareVersion.isGreater(latest.getFirmwareVersion(), currentVersion)) {
			return;
		}
		upgrades.add(FirmwareUpgradeCandidate.builder()
			.firmwareId(latest.getId())
			.target(target.getValue())
			.currentVersion(currentVersion)
			.latestVersion(latest.getFirmwareVersion())
			.fileName(latest.getOriginalFileName())
			.fileSize(latest.getFileSize())
			.sha256(latest.getSha256())
			.build());
	}

	private MqttDevice requiredDevice(Long deviceRowId) {
		if (deviceRowId == null) {
			throw new ServiceException("设备记录 ID 不能为空");
		}
		MqttDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<MqttDevice>()
			.eq(MqttDevice::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(MqttDevice::getId, deviceRowId)
			.eq(MqttDevice::getIsDeleted, 0)
			.last("limit 1"));
		if (device == null) {
			throw new ServiceException("设备不存在");
		}
		return device;
	}

	private boolean hasKnownCurrentVersion(MqttDevice device) {
		return FirmwareVersion.isValid(device.getApplicationVersion())
			|| FirmwareVersion.isValid(device.getRootfsVersion());
	}

	private String currentVersion(MqttDevice device, FirmwareTarget target) {
		return target == FirmwareTarget.ROOTFS
			? device.getRootfsVersion() : device.getApplicationVersion();
	}

	private String rootMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return StringUtils.defaultIfBlank(current.getMessage(), current.getClass().getSimpleName());
	}

	private String platformDownloadUrl(String requestId, String messageId) {
		String baseUrl = StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getPlatformBaseUrl()), "/");
		try {
			URI uri = URI.create(baseUrl);
			if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
				|| StringUtils.isBlank(uri.getHost()) || uri.getQuery() != null || uri.getFragment() != null) {
				throw new IllegalArgumentException("invalid platform base URL");
			}
		} catch (RuntimeException exception) {
			throw new ServiceException("vlstream.firmware.platform-base-url 必须是摄像头可访问的 HTTP(S) 根地址");
		}
		return baseUrl + "/vlsDeviceFirmware/ota/" + requestId + "/" + messageId;
	}
}
