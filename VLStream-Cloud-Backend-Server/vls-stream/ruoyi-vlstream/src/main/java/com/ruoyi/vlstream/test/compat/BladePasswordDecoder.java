/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.compat;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Decodes the SM2 password format submitted by VLStream-Web.
 */
@Component
public class BladePasswordDecoder {

    private static final String ENCRYPT_PREFIX = "04";

    private final String publicKey;
    private final String privateKey;

    @Autowired
    public BladePasswordDecoder(BladeAuthProperties properties) {
        this(properties.getPublicKey(), properties.getPrivateKey());
    }

    public BladePasswordDecoder(String publicKey, String privateKey) {
        this.publicKey = trimToNull(publicKey);
        this.privateKey = trimToNull(privateKey);
    }

    /**
     * Decode the frontend SM2 ciphertext into the plaintext password used by RuoYi login.
     */
    public String decode(String rawPassword) {
        String password = trimToNull(rawPassword);
        if (password == null) {
            return rawPassword;
        }
        if (publicKey == null || privateKey == null) {
            return "";
        }

        SM2 sm2 = SmUtil.sm2(privateKey, publicKey);
        sm2.setMode(SM2Engine.Mode.C1C2C3);
        String decoded = tryDecrypt(sm2, startsWithIgnoreCase(password, ENCRYPT_PREFIX)
            ? password : ENCRYPT_PREFIX + password);
        if (decoded == null && startsWithIgnoreCase(password, ENCRYPT_PREFIX)) {
            // sm-crypto mode 0 omits the uncompressed-point marker. Its random
            // first coordinate can naturally begin with 04, so retry with the
            // marker instead of treating those two ciphertext digits as it.
            decoded = tryDecrypt(sm2, ENCRYPT_PREFIX + password);
        }
        return decoded == null ? "" : decoded;
    }

    private String tryDecrypt(SM2 sm2, String encrypted) {
        try {
            String decoded = sm2.decryptStr(encrypted, KeyType.PrivateKey, StandardCharsets.UTF_8);
            byte[] decodedBytes = decoded.getBytes(StandardCharsets.UTF_8);
            byte[] signature = sm2.sign(decodedBytes);
            return sm2.verify(decodedBytes, signature) ? decoded : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.length() >= prefix.length()
            && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
