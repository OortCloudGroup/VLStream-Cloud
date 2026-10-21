/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Issues and validates direct-to-MinIO uploads for device event images and MP4 clips.
 */
@Service
@Slf4j
public class DeviceMediaUploadService {

	private static final String STATUS_ISSUED = "ISSUED";
	private static final String STATUS_BOUND = "BOUND";

	@Resource
	private VlsDeviceMediaProperties properties;

	@Resource
	private WvpVlStreamDeviceResolver wvpDeviceResolver;

	@Resource
	private VlsDeviceMediaUploadMapper uploadMapper;

	public DeviceMediaUploadResponse issueUploadUrl(DeviceMediaUploadRequest request) {
		if (!Boolean.TRUE.equals(properties.getAllowUnauthenticated())) {
			throw new ServiceException("设备媒体上传接口未启用；生产环境必须先配置设备身份认证");
		}
		validateRequest(request);
		DeviceInfo device = wvpDeviceResolver.resolve(request.getDeviceId());
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
				.uploadUrl(generateUploadUrl(ossClient, objectKey, request.getContentType(), ttlSeconds))
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
			|| !StringUtils.equals(upload.getTenantId(), TenantContextHolder.getTenantId())
			|| !StringUtils.equals(upload.getObjectKey(), objectKey)) {
			throw new ServiceException("媒体对象与设备或 objectKey 不匹配");
		}
		if (!StringUtils.equalsIgnoreCase(upload.getSha256(), sha256)) {
			throw new ServiceException("事件中的媒体 SHA-256 与申请上传时不一致");
		}
		if (STATUS_BOUND.equals(upload.getUploadStatus())) {
			if (StringUtils.equals(upload.getBoundEventMessageId(), eventMessageId)) {
				return upload;
			}
			throw new ServiceException("媒体对象已经绑定其他事件");
		}
		if (!STATUS_ISSUED.equals(upload.getUploadStatus())) {
			throw new ServiceException("媒体上传状态不允许绑定");
		}

		OssClient client = storedClient(upload);
		if (!client.doesObjectExist(upload.getObjectKey())) {
			throw new ServiceException("MinIO 媒体尚未上传");
		}
		ObjectMetadata metadata = client.getObjectMetadata(upload.getObjectKey());
		if (metadata.getContentLength() != upload.getFileSize()) {
			throw new ServiceException("MinIO 媒体大小不匹配");
		}
		if (!StringUtils.equalsIgnoreCase(upload.getContentType(), metadata.getContentType())) {
			throw new ServiceException("对象 Content-Type 与申请上传时不一致");
		}
		DeviceEventMediaInspector.validate(client, upload, maxBytes(upload.getContentType()));

		Date now = new Date();
		int updated = uploadMapper.update(null, new LambdaUpdateWrapper<DeviceMediaUpload>()
			.eq(DeviceMediaUpload::getTenantId, upload.getTenantId())
			.eq(DeviceMediaUpload::getDeviceId, deviceId)
			.eq(DeviceMediaUpload::getMediaId, mediaId)
			.eq(DeviceMediaUpload::getUploadStatus, STATUS_ISSUED)
			.set(DeviceMediaUpload::getUploadStatus, STATUS_BOUND)
			.set(DeviceMediaUpload::getBoundEventMessageId, eventMessageId)
			.set(DeviceMediaUpload::getUpdateTime, now));
		if (updated == 0) {
			DeviceMediaUpload current = getByMediaId(mediaId);
			if (current == null || !STATUS_BOUND.equals(current.getUploadStatus())
				|| !StringUtils.equals(current.getBoundEventMessageId(), eventMessageId)) {
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
		if (StringUtils.isBlank(mediaId) || StringUtils.isBlank(TenantContextHolder.getTenantId())) {
			return null;
		}
		return uploadMapper.selectOne(new LambdaQueryWrapper<DeviceMediaUpload>()
			.eq(DeviceMediaUpload::getTenantId, TenantContextHolder.getTenantId())
			.eq(DeviceMediaUpload::getMediaId, mediaId)
			.last("limit 1"));
	}

	public String getPrivateViewUrl(String mediaId, int seconds) {
		return getPrivateViewUrl(mediaId, seconds, false);
	}

	public String getPrivateViewUrl(String mediaId, int seconds, boolean download) {
		DeviceMediaUpload upload = getByMediaId(mediaId);
		if (upload == null || !STATUS_BOUND.equals(upload.getUploadStatus())) {
			throw new ServiceException("事件媒体不存在或尚未绑定");
		}
		int safeSeconds = Math.max(60, Math.min(seconds, 3600));
        if (download) return storedClient(upload).getPrivateUrl(viewObjectKey(upload), safeSeconds,
            properties.getPublicEndpoint(), true);
        return signViewUrl(upload, safeSeconds);
	}

    /** Resolve one page's distinct media references with one tenant-scoped query. */
    public Map<String, String> getPrivateViewUrls(String tenantId, Collection<String> mediaIds, int seconds) {
        if (StringUtils.isBlank(tenantId) || mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> distinctIds = new LinkedHashSet<String>();
        for (String mediaId : mediaIds) {
            if (StringUtils.isNotBlank(mediaId)) {
                distinctIds.add(mediaId);
            }
        }
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DeviceMediaUpload> uploads = uploadMapper.selectList(new LambdaQueryWrapper<DeviceMediaUpload>()
            .eq(DeviceMediaUpload::getTenantId, tenantId)
            .in(DeviceMediaUpload::getMediaId, distinctIds));
        int safeSeconds = Math.max(60, Math.min(seconds, 3600));
        Map<String, String> urls = new LinkedHashMap<String, String>();
        for (DeviceMediaUpload upload : uploads) {
            if (!tenantId.equals(upload.getTenantId()) || !STATUS_BOUND.equals(upload.getUploadStatus())) {
                continue;
            }
            try {
                urls.put(upload.getMediaId(), signViewUrl(upload, safeSeconds));
            } catch (RuntimeException exception) {
                log.warn("Could not sign private VLS event media {}", upload.getMediaId(), exception);
            }
        }
        return urls;
    }

    String signViewUrl(DeviceMediaUpload upload, int seconds) {
        return generatePrivateViewUrl(storedClient(upload), viewObjectKey(upload), seconds);
    }

    static String verifiedVideoKey(DeviceMediaUpload upload) {
        return upload.getObjectKey() + ".verified.mp4";
    }

    private String viewObjectKey(DeviceMediaUpload upload) {
        return isVideoType(upload.getContentType()) ? verifiedVideoKey(upload) : upload.getObjectKey();
    }

    OssClient storedClient(DeviceMediaUpload upload) {
        return OssFactory.instance(upload.getOssConfigKey());
    }

	String generateUploadUrl(OssClient client, String objectKey, String contentType, int ttlSeconds) {
		return client.getPresignedPutUrl(objectKey, contentType, ttlSeconds, properties.getPublicEndpoint());
	}

	String generatePrivateViewUrl(OssClient client, String objectKey, int ttlSeconds) {
		return client.getPrivateUrl(objectKey, ttlSeconds, properties.getPublicEndpoint());
	}

	/** Read one validated private event image for server-side vision inference. */
	public byte[] readBoundImage(String mediaId, int maxBytes) {
		DeviceMediaUpload upload = getByMediaId(mediaId);
		if (upload == null || !STATUS_BOUND.equals(upload.getUploadStatus())) {
			throw new ServiceException("事件媒体不存在或尚未绑定");
		}
		if (!isSupportedImageType(upload.getContentType())) {
			throw new ServiceException("视频不能作为图片复核，请同时上报抓拍图片");
		}
		int safeMax = Math.max(1024, maxBytes);
		try (InputStream input = OssFactory.instance(upload.getOssConfigKey())
			.getObjectContent(upload.getObjectKey());
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int total = 0;
			int length;
			while ((length = input.read(buffer)) >= 0) {
				if (length == 0) {
					continue;
				}
				total += length;
				if (total > safeMax) {
					throw new ServiceException("事件图片超过大模型复核大小限制");
				}
				output.write(buffer, 0, length);
			}
			return output.toByteArray();
		} catch (ServiceException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ServiceException("读取事件图片失败：" + exception.getMessage());
		}
	}

	private void validateRequest(DeviceMediaUploadRequest request) {
		if (request == null) {
			throw new ServiceException("上传参数不能为空");
		}
		if (!StringUtils.defaultString(request.getDeviceId()).matches("[A-Za-z0-9._-]{1,100}")) {
			throw new ServiceException("deviceId 格式不合法");
		}
		if (!isSupportedImageType(request.getContentType()) && !isVideoType(request.getContentType())) {
			throw new ServiceException("仅支持 image/jpeg、image/png、image/webp、video/mp4");
		}
		request.setContentType(request.getContentType().toLowerCase(Locale.ROOT));
		long maxBytes = maxBytes(request.getContentType());
		if (request.getFileSize() == null || request.getFileSize() <= 0 || request.getFileSize() > maxBytes) {
			throw new ServiceException("媒体大小不合法，最大允许 " + maxBytes + " 字节");
		}
		if (!StringUtils.defaultString(request.getSha256()).matches("(?i)[0-9a-f]{64}")) {
			throw new ServiceException("sha256 必须是 64 位十六进制字符串");
		}
	}

	static boolean isVideoType(String contentType) {
		return "video/mp4".equalsIgnoreCase(contentType);
	}

	private long maxBytes(String contentType) {
		Long configured = isVideoType(contentType) ? properties.getMaxVideoBytes() : properties.getMaxImageBytes();
		long fallback = (isVideoType(contentType) ? 100L : 10L) * 1024L * 1024L;
		return configured == null || configured <= 0 ? fallback : configured;
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
		if (isVideoType(contentType)) {
			return ".mp4";
		}
		if ("image/png".equalsIgnoreCase(contentType)) {
			return ".png";
		}
		if ("image/webp".equalsIgnoreCase(contentType)) {
			return ".webp";
		}
		return ".jpg";
	}

	OssClient ossClient() {
		return StringUtils.isBlank(properties.getOssConfigKey())
			? OssFactory.instance()
			: OssFactory.instance(properties.getOssConfigKey());
	}

	private int defaultInt(Integer value, int fallback) {
		return value == null || value <= 0 ? fallback : value;
	}
}
