package com.ruoyi.vlstream.test.vlstream.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** A deployment-owned key, with authenticated encryption and a fresh nonce per credential. */
@Service
public class ComputeCredentialCipher {
    private final java.util.function.Supplier<byte[]> keys;
    @Autowired public ComputeCredentialCipher(ComputeKeyStore store) { this.keys = store::key; }
    public ComputeCredentialCipher(String key) { this.keys = () -> ComputeKeyStore.checked(key.getBytes(StandardCharsets.UTF_8)); }
    public String encrypt(String plain) {
        try {
            byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
            byte[] value = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] output = Arrays.copyOf(nonce, nonce.length + value.length);
            System.arraycopy(value, 0, output, nonce.length, value.length);
            return "v1:" + Base64.getEncoder().encodeToString(output);
        } catch (Exception e) { throw new IllegalStateException("算力凭据加密失败，请检查服务端持久化密钥存储", e); }
    }
    public String decrypt(String encrypted) {
        try {
            if (encrypted == null || !encrypted.startsWith("v1:")) throw new IllegalArgumentException();
            byte[] value = Base64.getDecoder().decode(encrypted.substring(3));
            if (value.length < 29) throw new IllegalArgumentException();
            return new String(cipher(Cipher.DECRYPT_MODE, Arrays.copyOf(value, 12)).doFinal(Arrays.copyOfRange(value, 12, value.length)), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("算力凭据解密失败，请核对部署密钥", e); }
    }
    private Cipher cipher(int mode, byte[] nonce) throws Exception {
        byte[] bytes = keys.get();
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) throw new IllegalArgumentException("部署密钥须为 16、24 或 32 字节");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(bytes, "AES"), new GCMParameterSpec(128, nonce));
        return cipher;
    }
}
