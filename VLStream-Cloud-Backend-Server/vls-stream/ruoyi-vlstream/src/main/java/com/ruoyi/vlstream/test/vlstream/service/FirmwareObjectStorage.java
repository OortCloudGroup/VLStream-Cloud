/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.enumd.AccessPolicyType;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;

/** MinIO operations used by the firmware repository. */
@Component
@RequiredArgsConstructor
public class FirmwareObjectStorage {

	private final VlsFirmwareProperties properties;

	public String configKey() {
		return client().getConfigKey();
	}

	public String presignedPutUrl(String objectKey, String contentType, int ttlSeconds) {
		return client().getPresignedPutUrl(objectKey, contentType, ttlSeconds, properties.getPublicEndpoint());
	}

	public String presignedGetUrl(String configKey, String objectKey, int ttlSeconds) {
		return privateClient(configKey)
			.getPrivateUrl(objectKey, ttlSeconds, properties.getPublicEndpoint());
	}

	public String verifyAndCalculateSha256(String configKey, String objectKey, long expectedSize) {
		OssClient client = privateClient(configKey);
		if (!client.doesObjectExist(objectKey)) {
			throw new ServiceException("MinIO 固件包尚未上传");
		}
		ObjectMetadata metadata = client.getObjectMetadata(objectKey);
		if (metadata.getContentLength() != expectedSize) {
			throw new ServiceException("MinIO 固件包大小不匹配");
		}
		try (InputStream input = client.getObjectContent(objectKey)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
			return toHex(digest.digest());
		} catch (Exception exception) {
			throw new ServiceException("校验 MinIO 固件包失败：" + exception.getMessage());
		}
	}

	public InputStream openObject(String configKey, String objectKey) {
		return privateClient(configKey).getObjectContent(objectKey);
	}

	public void deleteIfExists(String configKey, String objectKey) {
		OssClient client = privateClient(configKey);
		if (client.doesObjectExist(objectKey)) {
			client.delete(objectKey);
		}
	}

	private OssClient client() {
		OssClient client = StringUtils.isBlank(properties.getOssConfigKey())
			? OssFactory.instance() : OssFactory.instance(properties.getOssConfigKey());
		ensurePrivate(client);
		return client;
	}

	private OssClient privateClient(String configKey) {
		OssClient client = OssFactory.instance(configKey);
		ensurePrivate(client);
		return client;
	}

	private void ensurePrivate(OssClient client) {
		if (client.getAccessPolicy() != AccessPolicyType.PRIVATE) {
			throw new ServiceException("固件存储必须使用 access_policy=0 的私有 OSS 配置");
		}
	}

	private String toHex(byte[] bytes) {
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			result.append(String.format("%02x", value & 0xff));
		}
		return result.toString();
	}
}
