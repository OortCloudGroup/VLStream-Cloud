package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceMediaUploadMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Verified worker evidence uses the same private media store/view contract as active safety. */
@Service
@RequiredArgsConstructor
public class CloudAiEvidenceStore {
    private final VlsDeviceMediaUploadMapper media;

    public String archive(Path local, String runId, String eventId, String role, String deviceId, long expectedSize, String expectedSha) throws IOException {
        String tenant = ModelArtifactObjectStore.tenant();
        if (!Arrays.asList("original", "annotated", "video").contains(role)) throw new IOException("Invalid evidence role");
        String type = "video".equals(role) ? "video/mp4" : "image/jpeg";
        long maximum = "video".equals(role) ? 256L * 1024 * 1024 : 16L * 1024 * 1024;
        if (expectedSize < 1 || expectedSize > maximum || Files.size(local) != expectedSize || expectedSha == null || !expectedSha.matches("[a-f0-9]{64}")) throw new IOException("Evidence size is invalid");
        try (InputStream input = Files.newInputStream(local)) { if (!expectedSha.equals(ModelArtifactObjectStore.digest(input))) throw new IOException("Evidence checksum mismatch"); }
        String id = UUID.nameUUIDFromBytes((tenant + "\n" + eventId + "\n" + role).getBytes(StandardCharsets.UTF_8)).toString();
        DeviceMediaUpload existing = media.selectOne(new LambdaQueryWrapper<DeviceMediaUpload>().eq(DeviceMediaUpload::getTenantId, tenant).eq(DeviceMediaUpload::getMediaId, id));
        if (existing != null) {
            if (!expectedSha.equals(existing.getSha256()) || !deviceId.equals(existing.getDeviceId()) || !("cloud-ai:" + eventId).equals(existing.getBoundEventMessageId())) throw new IOException("Evidence identity changed");
            return id;
        }
        OssClient client = OssFactory.instance();
        String object = "cloud-ai/" + ModelArtifactObjectStore.hash(tenant) + "/" + runId + "/" + eventId + "/" + role + ("video".equals(role) ? ".mp4" : ".jpg");
        client.uploadFile(local.toFile(), object, type);
        DeviceMediaUpload row = new DeviceMediaUpload(); row.setTenantId(tenant); row.setMediaId(id); row.setDeviceId(deviceId);
        row.setOssConfigKey(client.getConfigKey()); row.setObjectKey(object); row.setFileName(role + ("video".equals(role) ? ".mp4" : ".jpg"));
        row.setContentType(type); row.setFileSize(expectedSize); row.setSha256(expectedSha);
        row.setUploadStatus("BOUND"); row.setBoundEventMessageId("cloud-ai:" + eventId);
        row.setCreateTime(new Date()); row.setUpdateTime(new Date()); row.setExpiresAt(new Date());
        if (client.getObjectMetadata(object).getContentLength() != expectedSize) throw new IOException("Stored evidence length mismatch");
        DeviceEventMediaInspector.validate(client, row, maximum);
        try { media.insert(row); }
        catch (org.springframework.dao.DuplicateKeyException duplicate) {
            existing = media.selectOne(new LambdaQueryWrapper<DeviceMediaUpload>().eq(DeviceMediaUpload::getTenantId, tenant).eq(DeviceMediaUpload::getMediaId, id));
            if (existing == null || !expectedSha.equals(existing.getSha256())) throw duplicate;
        }
        return id;
    }
}
