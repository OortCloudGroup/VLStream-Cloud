/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 * rsa method
 *
 * @author zhonghuixiong
 */
public class LoginRsaUtils {

    /**
     * rsa
     *
     * @param encryptString
     * @return
     */
    public static String encrypt(String encryptString) {
        try {
            // PEM Convert to PublicKey object
            PublicKey publicKey = getPublicKeyFromPem(PlatformConstants.PEM_PUBLIC_KEY);

            // RSA/ECB/PKCS1Padding
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(encryptString.getBytes("UTF-8"));

            // after dataConvert to Base64
            String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
            System.out.println("加密后的数据: " + encryptedData);
            return encryptedData;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * PEM Convert to PublicKey object
     *
     * @param pemPublicKey PEM
     * @return PublicKey object
     */
    private static PublicKey getPublicKeyFromPem(String pemPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // PEM and
        String publicKeyPem = pemPublicKey.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

        // Base64 to array
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyPem);

        // X509EncodedKeySpec PublicKey object
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

}
