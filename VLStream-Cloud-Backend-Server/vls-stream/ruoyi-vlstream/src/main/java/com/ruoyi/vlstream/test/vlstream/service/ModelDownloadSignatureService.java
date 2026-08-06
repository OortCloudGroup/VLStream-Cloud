/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Generates and validates short-lived HMAC download signatures.
 */
@Service
public class ModelDownloadSignatureService {

	private static final String HMAC_SHA_256 = "HmacSHA256";

	@Resource
	private VlsModelDispatchProperties properties;

	public String sign(String requestId, long expiresAt) {
		String secret = properties.getSigningSecret();
		if (StringUtils.isBlank(secret)) {
			throw new IllegalStateException("VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET is not configured");
		}
		try {
			Mac mac = Mac.getInstance(HMAC_SHA_256);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
			return Hex.encodeHexString(mac.doFinal(value(requestId, expiresAt).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new IllegalStateException("Cannot sign model download URL", ex);
		}
	}

	public boolean verify(String requestId, long expiresAt, String signature) {
		if (StringUtils.isBlank(signature) || expiresAt < System.currentTimeMillis() / 1000L) {
			return false;
		}
		byte[] expected = sign(requestId, expiresAt).getBytes(StandardCharsets.US_ASCII);
		byte[] actual = signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII);
		return MessageDigest.isEqual(expected, actual);
	}

	private String value(String requestId, long expiresAt) {
		return requestId + ":" + expiresAt;
	}
}
