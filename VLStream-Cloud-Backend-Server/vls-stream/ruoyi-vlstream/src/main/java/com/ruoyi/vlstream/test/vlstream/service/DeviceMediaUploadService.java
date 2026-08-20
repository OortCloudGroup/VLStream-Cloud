/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceMediaUploadMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadResponse;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Issues and validates direct-to-MinIO uploads for device event images.
 */
@Service
public class DeviceMediaUploadService {

	private static final String STATUS_ISSUED = "ISSUED";
	private static final String STATUS_BOUND = "BOUND";

	@Resource
	private VlsDeviceMediaProperties properties;

	@Resource
	private TenantDeviceResolver tenantDeviceResolver;

	@Resource
	private VlsDeviceMediaUploadMapper uploadMapper;

	public DeviceMediaUploadResponse issueUploadUrl(DeviceMediaUploadRequest request) {
		if (!Boolean.TRUE.equals(properties.getAllowUnauthenticated())) {
			throw new ServiceException("设备媒体上传接口未启用；生产环境必须先配置设备身份认证");
		}
		validateRequest(request);
		DeviceInfo device = tenantDeviceResolver.resolveUnique(request.getDeviceId());
		String previousTenant = TenantContextHolder.getTenantId();
		TenantContextHolder.setTenantId(device.getTenantId());
		try {

			OssClient ossClient = ossClient();
			String mediaId = UUID.randomUUID().toString();
			String objectKey = buildObjectKey(request.getDeviceId(), mediaId, request.getContentType());
			int ttlSeconds = Math.max(60, Math.min(defaultInt(properties.getUploadUrlTtlSeconds(), 600), 3600));
			Date expiresAt = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);

			DeviceMediaUpload upload = new DeviceMediaUpload();
			upload.setTenantId(device.getTenantId());
			upload.setMediaId(mediaId);
			upload.setDeviceId(request.getDeviceId());
			upload.setOssConfigKey(ossClient.getConfigKey());
			upload.setObjectKey(objectKey);
			upload.setFileName(StringUtils.abbreviate(request.getFileName(), 255));
			upload.setContentType(request.getContentType());
			upload.setFileSize(request.getFileSize());
			upload.setSha256(StringUtils.lowerCase(request.getSha256(), Locale.ROOT));
			upload.setUploadStatus(STATUS_ISSUED);
			upload.setExpiresAt(expiresAt);
			upload.setCreateTime(new Date());
			upload.setUpdateTime(new Date());
			uploadMapper.insert(upload);

			return DeviceMediaUploadResponse.builder()
				.mediaId(mediaId)
				.objectKey(objectKey)
				.uploadUrl(ossClient.getPresignedPutUrl(objectKey, request.getContentType(), ttlSeconds))
				.expiresAt(Instant.ofEpochMilli(expiresAt.getTime()).toString())
				.requiredContentType(request.getContentType())
				.build();
		} finally {
			TenantContextHolder.setTenantId(previousTenant);
		}
	}

	/**
	 * Validates the object and atomically binds it to one MQTT event.
	 */
	public DeviceMediaUpload validateAndBind(String mediaId, String deviceId, String objectKey,
											 String sha256, String eventMessageId) {
		DeviceMediaUpload upload = getByMediaId(mediaId);
		if (upload == null) {
			throw new ServiceException("mediaId 不存在：" + mediaId);
		}
		if (!StringUtils.equals(upload.getDeviceId(), deviceId)
			|| !StringUtils.equals(upload.getObjectKey(), objectKey)) {
			throw new ServiceException("媒体对象与设备或 objectKey 不匹配");
		}
		if (STATUS_BOUND.equals(upload.getUploadStatus())) {
			if (StringUtils.equals(upload.getBoundEventMessageId(), eventMessageId)) {
				return upload;
			}
			throw new ServiceException("媒体对象已经绑定其他事件");
		}
		if (!StringUtils.equalsIgnoreCase(upload.getSha256(), sha256)) {
			throw new ServiceException("事件中的图片 SHA-256 与申请上传时不一致");
		}

		OssClient client = OssFactory.instance(upload.getOssConfigKey());
		if (!client.doesObjectExist(upload.getObjectKey())) {
			throw new ServiceException("MinIO 图片尚未上传");
		}
		ObjectMetadata metadata = client.getObjectMetadata(upload.getObjectKey());
		if (metadata.getContentLength() != upload.getFileSize()) {
			throw new ServiceException("MinIO 图片大小不匹配");
		}
		String actualSha256 = calculateSha256(client, upload.getObjectKey());
		if (!StringUtils.equalsIgnoreCase(upload.getSha256(), actualSha256)) {
			throw new ServiceException("MinIO 图片 SHA-256 校验失败");
		}

		Date now = new Date();
		int updated = uploadMapper.update(null, new LambdaUpdateWrapper<DeviceMediaUpload>()
			.eq(DeviceMediaUpload::getMediaId, mediaId)
			.eq(DeviceMediaUpload::getUploadStatus, STATUS_ISSUED)
			.set(DeviceMediaUpload::getUploadStatus, STATUS_BOUND)
			.set(DeviceMediaUpload::getBoundEventMessageId, eventMessageId)
			.set(DeviceMediaUpload::getUpdateTime, now));
		if (updated == 0) {
			DeviceMediaUpload current = getByMediaId(mediaId);
			if (current == null || !StringUtils.equals(current.getBoundEventMessageId(), eventMessageId)) {
				throw new ServiceException("媒体对象绑定事件失败");
			}
			return current;
		}
		upload.setUploadStatus(STATUS_BOUND);
		upload.setBoundEventMessageId(eventMessageId);
		upload.setUpdateTime(now);
		return upload;
	}

	public DeviceMediaUpload getByMediaId(String mediaId) {
		if (StringUtils.isBlank(mediaId)) {
			return null;
		}
		return uploadMapper.selectOne(new LambdaQueryWrapper<DeviceMediaUpload>()
			.eq(DeviceMediaUpload::getMediaId, mediaId)
			.last("limit 1"));
	}

	public String getPrivateViewUrl(String mediaId, int seconds) {
		DeviceMediaUpload upload = getByMediaId(mediaId);
		if (upload == null || !STATUS_BOUND.equals(upload.getUploadStatus())) {
			throw new ServiceException("事件图片不存在或尚未绑定");
		}
		int safeSeconds = Math.max(60, Math.min(seconds, 3600));
		return OssFactory.instance(upload.getOssConfigKey())
			.getPrivateUrl(upload.getObjectKey(), safeSeconds);
	}

	private void validateRequest(DeviceMediaUploadRequest request) {
		if (request == null) {
			throw new ServiceException("上传参数不能为空");
		}
		if (!request.getDeviceId().matches("[A-Za-z0-9._-]{1,100}")) {
			throw new ServiceException("deviceId 格式不合法");
		}
		if (!isSupportedImageType(request.getContentType())) {
			throw new ServiceException("仅支持 image/jpeg、image/png、image/webp");
		}
		long maxBytes = properties.getMaxImageBytes() == null
			? 10L * 1024L * 1024L : properties.getMaxImageBytes();
		if (request.getFileSize() == null || request.getFileSize() <= 0 || request.getFileSize() > maxBytes) {
			throw new ServiceException("图片大小不合法，最大允许 " + maxBytes + " 字节");
		}
		if (!StringUtils.defaultString(request.getSha256()).matches("(?i)[0-9a-f]{64}")) {
			throw new ServiceException("sha256 必须是 64 位十六进制字符串");
		}
	}

	private boolean isSupportedImageType(String contentType) {
		return "image/jpeg".equalsIgnoreCase(contentType)
			|| "image/png".equalsIgnoreCase(contentType)
			|| "image/webp".equalsIgnoreCase(contentType);
	}

	private String buildObjectKey(String deviceId, String mediaId, String contentType) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return "events/" + deviceId + "/" + dateFormat.format(new Date())
			+ "/" + mediaId + extension(contentType);
	}

	private String extension(String contentType) {
		if ("image/png".equalsIgnoreCase(contentType)) {
			return ".png";
		}
		if ("image/webp".equalsIgnoreCase(contentType)) {
			return ".webp";
		}
		return ".jpg";
	}

	private String calculateSha256(OssClient client, String objectKey) {
		try (InputStream inputStream = client.getObjectContent(objectKey)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			int length;
			while ((length = inputStream.read(buffer)) >= 0) {
				if (length > 0) {
					digest.update(buffer, 0, length);
				}
			}
			StringBuilder hex = new StringBuilder(64);
			for (byte value : digest.digest()) {
				hex.append(String.format("%02x", value & 0xff));
			}
			return hex.toString();
		} catch (Exception ex) {
			throw new ServiceException("读取 MinIO 图片进行 SHA-256 校验失败：" + ex.getMessage());
		}
	}

	private OssClient ossClient() {
		return StringUtils.isBlank(properties.getOssConfigKey())
			? OssFactory.instance()
			: OssFactory.instance(properties.getOssConfigKey());
	}

	private int defaultInt(Integer value, int fallback) {
		return value == null || value <= 0 ? fallback : value;
	}
}
