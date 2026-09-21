/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TunnelTokenService {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private final SecureRandom secureRandom = new SecureRandom();
    private final VlsTunnelProperties properties;

    public TunnelTokenService(VlsTunnelProperties properties) {
        this.properties = properties;
    }

    public String randomToken(int bytes) {
        byte[] value = new byte[Math.max(16, bytes)];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public String hash(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Token cannot be blank");
        }
        try {
            return Hex.encodeHexString(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public String deriveServiceToken(Long endpointId, Long generation) {
        String secret = properties.getServiceSigningSecret();
        if (StringUtils.length(secret) < 32) {
            throw new TunnelApiException(503, "隧道服务签名密钥未配置或长度不足");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            byte[] signed = mac.doFinal((endpointId + ":" + generation).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot derive rathole service token", exception);
        }
    }

    public boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8));
    }
}
