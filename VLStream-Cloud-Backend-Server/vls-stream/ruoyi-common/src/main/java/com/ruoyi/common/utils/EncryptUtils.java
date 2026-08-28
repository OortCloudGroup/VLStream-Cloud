/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SM2;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * full related
 *
 * @author
 */
public class EncryptUtils {
    /**
     *
     */
    public static final String PUBLIC_KEY = "publicKey";
    /**
     *
     */
    public static final String PRIVATE_KEY = "privateKey";

    /**
     * Base64
     *
     * @param data data
     * @return after
     */
    public static String encryptByBase64(String data) {
        return Base64.encode(data, StandardCharsets.UTF_8);
    }

    /**
     * Base64
     *
     * @param data data
     * @return after
     */
    public static String decryptByBase64(String data) {
        return Base64.decodeStr(data, StandardCharsets.UTF_8);
    }

    /**
     * AES
     *
     * @param data data
     * @param password
     * @return after , Base64
     */
    public static String encryptByAes(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES需要传入秘钥信息");
        }
        // aesalgorithm need to is 16 、24 、32
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度要求为16位、24位、32位");
        }
        return SecureUtil.aes(password.getBytes(StandardCharsets.UTF_8)).encryptBase64(data, StandardCharsets.UTF_8);
    }

    /**
     * AES
     *
     * @param data data
     * @param password
     * @return after
     */
    public static String decryptByAes(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES需要传入秘钥信息");
        }
        // aesalgorithm need to is 16 、24 、32
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度要求为16位、24位、32位");
        }
        return SecureUtil.aes(password.getBytes(StandardCharsets.UTF_8)).decryptStr(data, StandardCharsets.UTF_8);
    }

    /**
     * sm4
     *
     * @param data data
     * @param password
     * @return after , Base64
     */
    public static String encryptBySm4(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("SM4需要传入秘钥信息");
        }
        // sm4algorithm need to is 16
        int sm4PasswordLength = 16;
        if (sm4PasswordLength != password.length()) {
            throw new IllegalArgumentException("SM4秘钥长度要求为16位");
        }
        return SmUtil.sm4(password.getBytes(StandardCharsets.UTF_8)).encryptBase64(data, StandardCharsets.UTF_8);
    }

    /**
     * sm4
     *
     * @param data data
     * @param password
     * @return after
     */
    public static String decryptBySm4(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("SM4需要传入秘钥信息");
        }
        // sm4algorithm need to is 16
        int sm4PasswordLength = 16;
        if (sm4PasswordLength != password.length()) {
            throw new IllegalArgumentException("SM4秘钥长度要求为16位");
        }
        return SmUtil.sm4(password.getBytes(StandardCharsets.UTF_8)).decryptStr(data, StandardCharsets.UTF_8);
    }

    /**
     * sm2 need to and
     *
     * @return Map
     */
    public static Map<String, String> generateSm2Key() {
        Map<String, String> keyMap = new HashMap<>(2);
        SM2 sm2 = SmUtil.sm2();
        keyMap.put(PRIVATE_KEY, sm2.getPrivateKeyBase64());
        keyMap.put(PUBLIC_KEY, sm2.getPublicKeyBase64());
        return keyMap;
    }

    /**
     * sm2
     *
     * @param data data
     * @param publicKey
     * @return after , Base64
     */
    public static String encryptBySm2(String data, String publicKey) {
        if (StrUtil.isBlank(publicKey)) {
            throw new IllegalArgumentException("SM2需要传入公钥进行加密");
        }
        SM2 sm2 = SmUtil.sm2(null, publicKey);
        return sm2.encryptBase64(data, StandardCharsets.UTF_8, KeyType.PublicKey);
    }

    /**
     * sm2
     *
     * @param data data
     * @param privateKey
     * @return after
     */
    public static String decryptBySm2(String data, String privateKey) {
        if (StrUtil.isBlank(privateKey)) {
            throw new IllegalArgumentException("SM2需要传入私钥进行解密");
        }
        SM2 sm2 = SmUtil.sm2(privateKey, null);
        return sm2.decryptStr(data, KeyType.PrivateKey, StandardCharsets.UTF_8);
    }

    /**
     * RSA need to and
     *
     * @return Map
     */
    public static Map<String, String> generateRsaKey() {
        Map<String, String> keyMap = new HashMap<>(2);
        RSA rsa = SecureUtil.rsa();
        keyMap.put(PRIVATE_KEY, rsa.getPrivateKeyBase64());
        keyMap.put(PUBLIC_KEY, rsa.getPublicKeyBase64());
        return keyMap;
    }

    /**
     * rsa
     *
     * @param data data
     * @param publicKey
     * @return after , Base64
     */
    public static String encryptByRsa(String data, String publicKey) {
        if (StrUtil.isBlank(publicKey)) {
            throw new IllegalArgumentException("RSA需要传入公钥进行加密");
        }
        RSA rsa = SecureUtil.rsa(null, publicKey);
        return rsa.encryptBase64(data, StandardCharsets.UTF_8, KeyType.PublicKey);
    }

    /**
     * rsa
     *
     * @param data data
     * @param privateKey
     * @return after
     */
    public static String decryptByRsa(String data, String privateKey) {
        if (StrUtil.isBlank(privateKey)) {
            throw new IllegalArgumentException("RSA需要传入私钥进行解密");
        }
        RSA rsa = SecureUtil.rsa(privateKey, null);
        return rsa.decryptStr(data, KeyType.PrivateKey, StandardCharsets.UTF_8);
    }

    /**
     * md5
     *
     * @param data data
     * @return after , Hex
     */
    public static String encryptByMd5(String data) {
        return SecureUtil.md5(data);
    }

    /**
     * sha256
     *
     * @param data data
     * @return after , Hex
     */
    public static String encryptBySha256(String data) {
        return SecureUtil.sha256(data);
    }

    /**
     * sm3
     *
     * @param data data
     * @return after , Hex
     */
    public static String encryptBySm3(String data) {
        return SmUtil.sm3(data);
    }

}
