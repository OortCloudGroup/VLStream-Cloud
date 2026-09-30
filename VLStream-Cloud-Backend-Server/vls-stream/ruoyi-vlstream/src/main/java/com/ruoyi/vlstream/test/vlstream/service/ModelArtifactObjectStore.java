package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/** Verified, tenant-scoped objects. Never persists an expiring signed URL. */
@Service
@RequiredArgsConstructor
public class ModelArtifactObjectStore {
    private final JdbcTemplate jdbc;

    @Value("${vlstream.model-storage.temp-dir:${java.io.tmpdir}/vls-model-storage}")
    private String tempDir;

    public static String tenant() {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty()) throw new IllegalStateException("缺少模型存储租户上下文");
        return tenant;
    }

    public StoredArtifact find(String path) {
        return find(tenant(), path);
    }

    public StoredArtifact find(String tenant, String path) {
        if (tenant == null || tenant.trim().isEmpty()) throw new IllegalArgumentException("缺少模型存储租户");
        List<StoredArtifact> rows = jdbc.query("SELECT oss_config_key,object_key,file_name,file_size,sha256 FROM vls_model_artifact_storage WHERE tenant_id=? AND path_hash=? AND remote_path=? AND storage_state='READY'",
            (rs, n) -> new StoredArtifact(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getString(5)), tenant, hash(path), path);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void stream(StoredArtifact artifact, OutputStream output) throws IOException {
        try (InputStream input = client(artifact.configKey).getObjectContent(artifact.objectKey)) {
            byte[] buffer = new byte[65536];
            int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
        } catch (RuntimeException e) { throw new IOException("MinIO 模型读取失败", e); }
    }

    protected OssClient client(String configKey) {
        return configKey == null ? OssFactory.instance() : OssFactory.instance(configKey);
    }

    /** Returns false while another worker owns the lease or retry backoff is active. */
    public boolean archive(String path, String fileName, Source source) throws IOException {
        String tenant = tenant();
        if (find(tenant, path) != null) return true;
        String key = hash(path);
        String owner = UUID.randomUUID().toString();
        jdbc.update("INSERT IGNORE INTO vls_model_artifact_storage(tenant_id,path_hash,remote_path) VALUES(?,?,?)", tenant, key, path);
        if (jdbc.update("UPDATE vls_model_artifact_storage SET storage_state='COPYING',lease_owner=?,retry_after=DATE_ADD(NOW(),INTERVAL 6 HOUR),attempts=attempts+1 WHERE tenant_id=? AND path_hash=? AND storage_state<>'READY' AND (retry_after IS NULL OR retry_after<NOW())", owner, tenant, key) != 1) return false;
        Path local = null;
        String stage = "准备本地临时文件";
        try {
            Path directory = Paths.get(tempDir);
            Files.createDirectories(directory);
            local = Files.createTempFile(directory, "model-", ".part");
            stage = "读取并校验源文件";
            source.write(local);
            long size = Files.size(local);
            if (size == 0) throw new IOException("模型产物为空");
            String sha;
            try (InputStream input = Files.newInputStream(local)) { sha = digest(input); }
            OssClient storage = client(null);
            String object = "models/" + hash(tenant) + "/" + key + "/" + sha + "/" + safeName(fileName);
            stage = "上传 MinIO";
            storage.uploadFile(local.toFile(), object, "application/octet-stream");
            stage = "回读校验 MinIO";
            if (storage.getObjectMetadata(object).getContentLength() != size) throw new IOException("MinIO 模型大小校验失败");
            try (InputStream input = storage.getObjectContent(object)) {
                if (!sha.equals(digest(input))) throw new IOException("MinIO 模型 SHA-256 校验失败");
            }
            return jdbc.update("UPDATE vls_model_artifact_storage SET storage_state='READY',oss_config_key=?,object_key=?,file_name=?,file_size=?,sha256=?,lease_owner=NULL,retry_after=NULL,error_message=NULL WHERE tenant_id=? AND path_hash=? AND lease_owner=?",
                storage.getConfigKey(), object, fileName, size, sha, tenant, key, owner) == 1;
        } catch (Exception ex) {
            jdbc.update("UPDATE vls_model_artifact_storage SET storage_state='FAILED',lease_owner=NULL,retry_after=DATE_ADD(NOW(),INTERVAL 5 MINUTE),error_message=? WHERE tenant_id=? AND path_hash=? AND lease_owner=?",
                stage + "失败：" + ex.getClass().getSimpleName(), tenant, key, owner);
            throw new IOException("模型归档失败", ex);
        } finally {
            if (local != null) Files.deleteIfExists(local);
        }
    }

    static String safeName(String name) {
        return name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
    }

    public static String hash(String value) {
        try { return digest(new java.io.ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))); }
        catch (IOException e) { throw new IllegalStateException(e); }
    }

    static String digest(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest()) hex.append(String.format("%02x", value & 255));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public interface Source { void write(Path target) throws IOException; }

    @Getter
    @AllArgsConstructor
    public static class StoredArtifact {
        private final String configKey;
        private final String objectKey;
        private final String fileName;
        private final long fileSize;
        private final String sha256;
    }
}
