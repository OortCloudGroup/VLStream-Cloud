/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.compat.SingleTenant;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import com.ruoyi.vlstream.test.vlstream.enums.FirmwareTarget;
import com.ruoyi.vlstream.test.vlstream.mapper.DeviceFirmwareMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceFirmwareView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDownloadUrl;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwarePackageDownload;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUploadGrant;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceFirmware;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Firmware package maintenance workflow. */
@Service
@RequiredArgsConstructor
public class DeviceFirmwareService {

	private static final String STATUS_UPLOADING = "UPLOADING";
	private static final String STATUS_READY = "READY";
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	private static final String OTA_SUFFIX = ".ota";

	private final DeviceFirmwareMapper mapper;
	private final FirmwareObjectStorage storage;
	private final VlsFirmwareProperties properties;

	public Page<DeviceFirmwareView> page(long current, long size, String cameraModel, String target,
		String firmwareVersion) {
		long safeCurrent = Math.max(1, current);
		long safeSize = Math.max(1, Math.min(size, 100));
		Page<DeviceFirmware> source = mapper.selectPage(new Page<DeviceFirmware>(safeCurrent, safeSize),
			new LambdaQueryWrapper<DeviceFirmware>()
				.eq(DeviceFirmware::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
				.eq(DeviceFirmware::getIsDeleted, 0)
				.like(StringUtils.isNotBlank(cameraModel), DeviceFirmware::getCameraModel,
					StringUtils.trim(cameraModel))
				.eq(StringUtils.isNotBlank(target), DeviceFirmware::getTarget,
					StringUtils.lowerCase(StringUtils.trim(target)))
				.like(StringUtils.isNotBlank(firmwareVersion), DeviceFirmware::getFirmwareVersion,
					StringUtils.trim(firmwareVersion))
				.orderByDesc(DeviceFirmware::getCreateTime));
		Page<DeviceFirmwareView> result = new Page<DeviceFirmwareView>(safeCurrent, safeSize, source.getTotal());
		List<DeviceFirmwareView> records = new ArrayList<DeviceFirmwareView>(source.getRecords().size());
		for (DeviceFirmware firmware : source.getRecords()) {
			records.add(toView(firmware));
		}
		result.setRecords(records);
		return result;
	}

	public FirmwareUploadGrant issueUpload(FirmwareUploadRequest request) {
		ValidatedUpload upload = validate(request);
		DeviceFirmware duplicate = mapper.selectOne(new LambdaQueryWrapper<DeviceFirmware>()
			.eq(DeviceFirmware::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(DeviceFirmware::getCameraModel, upload.cameraModel)
			.eq(DeviceFirmware::getTarget, upload.target)
			.eq(DeviceFirmware::getFirmwareVersion, upload.firmwareVersion)
			.eq(DeviceFirmware::getIsDeleted, 0)
			.last("limit 1"));
		if (duplicate != null) {
			throw new ServiceException("该摄像头型号、升级目标和固件版本已存在，不允许覆盖旧固件");
		}

		int ttlSeconds = bounded(properties.getUploadUrlTtlSeconds(), 600, 60, 3600);
		Date expiresAt = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);
		String objectKey = "firmware/" + objectPathSegment(upload.cameraModel) + "/" + upload.target
			+ "/" + upload.firmwareVersion + "/"
			+ UUID.randomUUID().toString().replace("-", "") + upload.suffix;
		DeviceFirmware firmware = new DeviceFirmware();
		firmware.setTenantId(SingleTenant.DEFAULT_TENANT_ID);
		firmware.setCameraModel(upload.cameraModel);
		firmware.setTarget(upload.target);
		firmware.setFirmwareVersion(upload.firmwareVersion);
		firmware.setOssConfigKey(storage.configKey());
		firmware.setObjectKey(objectKey);
		firmware.setOriginalFileName(upload.fileName);
		firmware.setContentType(upload.contentType);
		firmware.setFileSize(upload.fileSize);
		firmware.setUploadStatus(STATUS_UPLOADING);
		firmware.setUploadExpiresAt(expiresAt);
		firmware.setStatus(1);
		firmware.setIsDeleted(0);
		firmware.setCreateTime(new Date());
		firmware.setUpdateTime(new Date());
		String uploadUrl = storage.presignedPutUrl(objectKey, upload.contentType, ttlSeconds);
		try {
			mapper.insert(firmware);
		} catch (DuplicateKeyException exception) {
			throw new ServiceException("该摄像头型号、升级目标和固件版本已存在，不允许覆盖旧固件");
		}
		return FirmwareUploadGrant.builder()
			.firmwareId(firmware.getId())
			.uploadUrl(uploadUrl)
			.requiredContentType(upload.contentType)
			.expiresAt(Instant.ofEpochMilli(expiresAt.getTime()).toString())
			.build();
	}

	public DeviceFirmwareView completeUpload(Long id) {
		DeviceFirmware firmware = required(id);
		if (STATUS_READY.equals(firmware.getUploadStatus())) {
			return toView(firmware);
		}
		String sha256 = storage.verifyAndCalculateSha256(firmware.getOssConfigKey(),
			firmware.getObjectKey(), firmware.getFileSize());
		firmware.setSha256(sha256);
		firmware.setUploadStatus(STATUS_READY);
		firmware.setUpdateTime(new Date());
		mapper.updateById(firmware);
		return toView(firmware);
	}

	public FirmwareDownloadUrl downloadUrl(Long id) {
		DeviceFirmware firmware = requiredReady(id);
		int ttlSeconds = bounded(properties.getDownloadUrlTtlSeconds(), 1800, 60, 86400);
		Date expiresAt = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);
		return FirmwareDownloadUrl.builder()
			.url(storage.presignedGetUrl(firmware.getOssConfigKey(), firmware.getObjectKey(), ttlSeconds))
			.fileName(firmware.getOriginalFileName())
			.expiresAt(Instant.ofEpochMilli(expiresAt.getTime()).toString())
			.build();
	}

	public DeviceFirmware requiredReady(Long id) {
		DeviceFirmware firmware = required(id);
		if (!STATUS_READY.equals(firmware.getUploadStatus()) || StringUtils.length(firmware.getSha256()) != 64) {
			throw new ServiceException("固件包尚未完成上传校验");
		}
		return firmware;
	}

	public DeviceFirmware findLatestReady(String cameraModel, String target) {
		if (StringUtils.isBlank(cameraModel)) {
			return null;
		}
		String normalizedTarget;
		try {
			normalizedTarget = FirmwareTarget.fromValue(target).getValue();
		} catch (IllegalArgumentException exception) {
			return null;
		}
		List<DeviceFirmware> candidates = mapper.selectList(new LambdaQueryWrapper<DeviceFirmware>()
			.eq(DeviceFirmware::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(DeviceFirmware::getCameraModel, StringUtils.trim(cameraModel))
			.eq(DeviceFirmware::getTarget, normalizedTarget)
			.eq(DeviceFirmware::getUploadStatus, STATUS_READY)
			.eq(DeviceFirmware::getIsDeleted, 0)
			.le(DeviceFirmware::getFileSize, maxPackageBytes()));
		DeviceFirmware latest = null;
		for (DeviceFirmware candidate : candidates) {
			if (!FirmwareVersion.isValid(candidate.getFirmwareVersion())) {
				continue;
			}
			if (latest == null
				|| FirmwareVersion.compare(candidate.getFirmwareVersion(), latest.getFirmwareVersion()) > 0) {
				latest = candidate;
			}
		}
		return latest;
	}

	public String otaDownloadUrl(DeviceFirmware firmware) {
		int ttlSeconds = bounded(properties.getOtaDownloadUrlTtlSeconds(), 7200, 300, 86400);
		return storage.presignedGetUrl(firmware.getOssConfigKey(), firmware.getObjectKey(), ttlSeconds);
	}

	public FirmwarePackageDownload openOtaPackage(Long firmwareId) {
		DeviceFirmware firmware = requiredReady(firmwareId);
		return FirmwarePackageDownload.builder()
			.inputStream(storage.openObject(firmware.getOssConfigKey(), firmware.getObjectKey()))
			.fileName(firmware.getOriginalFileName())
			.contentType(StringUtils.defaultIfBlank(firmware.getContentType(), DEFAULT_CONTENT_TYPE))
			.fileSize(firmware.getFileSize())
			.sha256(firmware.getSha256())
			.build();
	}

	public int otaDownloadTtlSeconds() {
		return bounded(properties.getOtaDownloadUrlTtlSeconds(), 7200, 300, 86400);
	}

	public void delete(Long id) {
		DeviceFirmware firmware = required(id);
		storage.deleteIfExists(firmware.getOssConfigKey(), firmware.getObjectKey());
		if (mapper.deletePermanently(id, SingleTenant.DEFAULT_TENANT_ID) != 1) {
			throw new ServiceException("固件记录删除失败");
		}
	}

	private DeviceFirmware required(Long id) {
		if (id == null) {
			throw new ServiceException("固件 ID 不能为空");
		}
		DeviceFirmware firmware = mapper.selectOne(new LambdaQueryWrapper<DeviceFirmware>()
			.eq(DeviceFirmware::getId, id)
			.eq(DeviceFirmware::getTenantId, SingleTenant.DEFAULT_TENANT_ID)
			.eq(DeviceFirmware::getIsDeleted, 0)
			.last("limit 1"));
		if (firmware == null) {
			throw new ServiceException("固件记录不存在");
		}
		return firmware;
	}

	private ValidatedUpload validate(FirmwareUploadRequest request) {
		if (request == null) {
			throw new ServiceException("固件上传参数不能为空");
		}
		String cameraModel = StringUtils.trim(request.getCameraModel());
		if (StringUtils.isBlank(cameraModel) || cameraModel.length() > 128
			|| cameraModel.indexOf('/') >= 0 || cameraModel.indexOf('\\') >= 0) {
			throw new ServiceException("摄像头型号不能为空、不能超过 128 个字符且不能包含路径分隔符");
		}
		String target;
		try {
			target = FirmwareTarget.fromValue(request.getTarget()).getValue();
		} catch (IllegalArgumentException exception) {
			throw new ServiceException("升级目标必须是 application 或 rootfs");
		}
		String firmwareVersion = StringUtils.trim(request.getFirmwareVersion());
		if (!FirmwareVersion.isValid(firmwareVersion)) {
			throw new ServiceException("固件版本号必须使用至少三段的纯数字点分格式，例如 1.0.1.14");
		}
		String fileName = normalizeFileName(request.getFileName());
		String suffix = supportedSuffix(fileName);
		long fileSize = request.getFileSize() == null ? 0L : request.getFileSize();
		long maxBytes = maxPackageBytes();
		if (fileSize <= 0 || fileSize > maxBytes) {
			throw new ServiceException("固件包大小必须大于 0 且不超过 " + maxBytes + " 字节");
		}
		String contentType = StringUtils.defaultIfBlank(StringUtils.trim(request.getContentType()),
			DEFAULT_CONTENT_TYPE);
		return new ValidatedUpload(cameraModel, target, firmwareVersion, fileName, suffix, contentType, fileSize);
	}

	private String normalizeFileName(String fileName) {
		String normalized = StringUtils.defaultString(fileName).replace('\\', '/');
		normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		if (normalized.length() == 0 || normalized.length() > 255) {
			throw new ServiceException("固件包文件名不能为空且不能超过 255 个字符");
		}
		return normalized;
	}

	private String supportedSuffix(String fileName) {
		String lowerName = fileName.toLowerCase(Locale.ROOT);
		if (lowerName.endsWith(OTA_SUFFIX)) {
			return OTA_SUFFIX;
		}
		throw new ServiceException("仅支持包含 manifest 的 .ota 固件包");
	}

	private long maxPackageBytes() {
		return properties.getMaxPackageBytes() == null
			? 160L * 1024L * 1024L : properties.getMaxPackageBytes();
	}

	private String objectPathSegment(String value) {
		return StringUtils.trim(value).replace(' ', '_');
	}

	private int bounded(Integer value, int defaultValue, int minimum, int maximum) {
		int actual = value == null ? defaultValue : value;
		return Math.max(minimum, Math.min(actual, maximum));
	}

	private DeviceFirmwareView toView(DeviceFirmware firmware) {
		return DeviceFirmwareView.builder()
			.id(firmware.getId())
			.cameraModel(firmware.getCameraModel())
			.target(firmware.getTarget())
			.firmwareVersion(firmware.getFirmwareVersion())
			.originalFileName(firmware.getOriginalFileName())
			.fileSize(firmware.getFileSize())
			.sha256(firmware.getSha256())
			.uploadStatus(firmware.getUploadStatus())
			.createTime(firmware.getCreateTime())
			.build();
	}

	private static class ValidatedUpload {
		private final String cameraModel;
		private final String target;
		private final String firmwareVersion;
		private final String fileName;
		private final String suffix;
		private final String contentType;
		private final long fileSize;

		private ValidatedUpload(String cameraModel, String target, String firmwareVersion, String fileName,
			String suffix, String contentType, long fileSize) {
			this.cameraModel = cameraModel;
			this.target = target;
			this.firmwareVersion = firmwareVersion;
			this.fileName = fileName;
			this.suffix = suffix;
			this.contentType = contentType;
			this.fileSize = fileSize;
		}
	}
}
