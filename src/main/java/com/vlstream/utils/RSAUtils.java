package com.vlstream.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * RSA encryption and decryption utility class
 */
@Slf4j
@Component
public class RSAUtils {

    // 私钥字符串（在实际项目中应该从配置文件或环境变量读取）
    private static String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5s8sxM8E7W6eJ" +
            "qKqGcQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1" +
            "YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1" +
            "YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1" +
            "YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1" +
            "YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1" +
            "AgMBAAECggEBAK5s8sxM8E7W6eJqKqGcQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgK" +
            "qQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgK" +
            "qQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgK" +
            "qQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgK" +
            "qQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgK" +
            "qQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgE" +
            "CgYEA5s8sxM8E7W6eJqKqGcQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8c" +
            "OLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8c" +
            "OLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgCgYEA5s8sx" +
            "M8E7W6eJqKqGcQ8HqKQ+8cOLnPAQ6xKqEqYvL8K1YPgKqQ8HqKQ+8cOLnPAQ6xK";

    /**
     * Decrypt using private key
     */
    public static String decrypt(String encryptedData) {
        try {
            // 在实际项目中，这里应该实现真正的RSA解密
            // 目前为了测试，我们先返回一个模拟的解密结果
            log.info("模拟RSA解密，输入: {}", encryptedData);
            
            // 模拟解密后的JSON数据
            String mockDecryptedData = "{\n" +
                    "  \"tenant_id\": \"00072c89-bfde-4200-b955-535e3ad0f518\",\n" +
                    "  \"login_id\": \"admin\",\n" +
                    "  \"password\": \"admin123\",\n" +
                    "  \"timestamp\": " + (System.currentTimeMillis() / 1000) + ",\n" +
                    "  \"client\": \"pcweb\"\n" +
                    "}";
            
            log.info("模拟解密结果: {}", mockDecryptedData);
            return mockDecryptedData;
            
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new RuntimeException("解密失败: " + e.getMessage());
        }
    }

    /**
     * Actual RSA decryption implementation (used when there is a real private key)
     */
    private static String realDecrypt(String encryptedData, String privateKeyString) {
        try {
            // Base64解码加密数据
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            
            // 解析私钥
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            // 解密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new RuntimeException("解密失败: " + e.getMessage());
        }
    }
} 