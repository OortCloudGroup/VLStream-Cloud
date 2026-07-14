/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.utils;

import com.ruoyi.common.constant.PlatformConstants;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * rsa加密方法
 *
 * @author zhonghuixiong
 */
public class LoginRsaUtils {

    /**
     * rsa加密
     *
     * @param encryptString
     * @return
     */
    public static String encrypt(String encryptString) {
        try {
            // 将 PEM 格式的公钥转换为 PublicKey 对象
            PublicKey publicKey = getPublicKeyFromPem(PlatformConstants.PEM_PUBLIC_KEY);

            // 使用 RSA/ECB/PKCS1Padding 进行加密
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(encryptString.getBytes("UTF-8"));

            // 将加密后的数据转换为 Base64 字符串
            String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
            System.out.println("加密后的数据: " + encryptedData);
            return encryptedData;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将 PEM 格式的公钥字符串转换为 PublicKey 对象
     *
     * @param pemPublicKey PEM 格式的公钥字符串
     * @return PublicKey 对象
     */
    private static PublicKey getPublicKeyFromPem(String pemPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 去掉 PEM 格式的头部和尾部
        String publicKeyPem = pemPublicKey.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

        // 将 Base64 编码的公钥字符串解码为字节数组
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyPem);

        // 使用 X509EncodedKeySpec 创建 PublicKey 对象
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

}
