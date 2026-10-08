package com.ruoyi.vlstream.test.vlstream.compute;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/** Deployment-wide fingerprint only; the database never holds the encryption key. */
@Service
public class ComputeKeyRegistry {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public ComputeKeyRegistry(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        transaction = new TransactionTemplate(manager);
        // A later failed node save must not roll back the binding of a cached key.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean requiresExistingKey() {
        return fingerprint() != null || !legacyCredentials().isEmpty();
    }

    public void validate(byte[] key) {
        String registered = fingerprint();
        if (registered != null) {
            if (!registered.equals(hash(key))) throw new IllegalStateException("算力密钥与数据库指纹不一致，请恢复原密钥或让所有副本共享密钥目录");
            return;
        }
        for (String encrypted : legacyCredentials()) {
            try {
                if (encrypted == null || !encrypted.startsWith("v1:")) throw new IllegalArgumentException();
                byte[] value = Base64.getDecoder().decode(encrypted.substring(3));
                if (value.length < 29) throw new IllegalArgumentException();
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Arrays.copyOf(value, 12)));
                byte[] plain = cipher.doFinal(Arrays.copyOfRange(value, 12, value.length));
                Arrays.fill(plain, (byte) 0);
            } catch (Exception e) {
                throw new IllegalStateException("现有算力凭据无法用该密钥解密，请恢复原部署密钥", e);
            }
        }
    }

    public void register(byte[] key) {
        transaction.execute(status -> {
            validate(key);
            jdbc.update("INSERT IGNORE INTO vls_compute_key_registry(id,key_sha256) VALUES(1,?)", hash(key));
            // The unique row serializes first use across replicas, including separate filesystems.
            // A locking read sees the winning insert under REPEATABLE READ. Shared locks
            // avoid duplicate-insert readers deadlocking while upgrading to exclusive locks.
            String registered = jdbc.queryForObject("SELECT key_sha256 FROM vls_compute_key_registry WHERE id=1 LOCK IN SHARE MODE", String.class);
            if (!hash(key).equals(registered)) throw new IllegalStateException("算力副本使用了不同密钥，请共享同一个持久化密钥目录");
            return null;
        });
    }

    private String fingerprint() {
        List<String> values = jdbc.queryForList("SELECT key_sha256 FROM vls_compute_key_registry WHERE id=1", String.class);
        return values.isEmpty() ? null : values.get(0);
    }

    private List<String> legacyCredentials() {
        return jdbc.queryForList("SELECT password_cipher FROM vls_compute_node ORDER BY id LIMIT 1", String.class);
    }

    private static String hash(byte[] key) {
        try {
            StringBuilder value = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(key)) value.append(String.format("%02x", b & 255));
            return value.toString();
        } catch (Exception e) { throw new IllegalStateException("无法校验算力密钥指纹", e); }
    }
}
