package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.properties.OssProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceMediaUploadMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadResponse;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Opt-in, loopback-only disposable MinIO. Never accepts a non-local endpoint or real credentials. */
@Tag("dev")
@EnabledIfSystemProperty(named = "vls.test.minio.endpoint", matches = "http://127\\.0\\.0\\.1:[0-9]+")
class DeviceMediaMinioIntegrationTest {
    @Test void realPresignedPutVerifyBindGetAndAttachmentDownload() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "minio"), DeviceMediaUpload.class);
        String endpoint = System.getProperty("vls.test.minio.endpoint");
        OssProperties oss = new OssProperties(); oss.setEndpoint(endpoint); oss.setAccessKey("vlsmp4test");
        oss.setSecretKey("isolated-mp4-test-only"); oss.setBucketName("event-mp4-isolated"); oss.setIsHttps("N");
        oss.setAccessPolicy("0");
        OssClient client = new OssClient("isolated", oss);
        VlsDeviceMediaProperties config = new VlsDeviceMediaProperties(); config.setAllowUnauthenticated(true);
        config.setPublicEndpoint(endpoint);
        DeviceMediaUploadService service = spy(new DeviceMediaUploadService());
        VlsDeviceMediaUploadMapper mapper = mock(VlsDeviceMediaUploadMapper.class);
        WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
        DeviceInfo device = new DeviceInfo(); device.setTenantId("isolated-tenant"); device.setDeviceId("SYNTHETIC-MINIO-CAM");
        when(resolver.resolve("SYNTHETIC-MINIO-CAM")).thenReturn(device);
        ReflectionTestUtils.setField(service, "properties", config);
        ReflectionTestUtils.setField(service, "uploadMapper", mapper);
        ReflectionTestUtils.setField(service, "wvpDeviceResolver", resolver);
        doReturn(client).when(service).ossClient(); doReturn(client).when(service).storedClient(any());
        AtomicReference<DeviceMediaUpload> saved = new AtomicReference<DeviceMediaUpload>();
        when(mapper.insert(any(DeviceMediaUpload.class))).thenAnswer(invocation -> { saved.set(invocation.getArgument(0)); return 1; });
        when(mapper.selectOne(any())).thenAnswer(invocation -> saved.get());
        when(mapper.update(isNull(), any())).thenReturn(1);
        byte[] bytes = Files.readAllBytes(Paths.get(getClass().getResource("/event-media.mp4").toURI()));
        DeviceMediaUploadRequest request = new DeviceMediaUploadRequest(); request.setDeviceId("SYNTHETIC-MINIO-CAM");
        request.setFileName("event.mp4"); request.setContentType("video/mp4"); request.setFileSize((long) bytes.length);
        request.setSha256("dfd3fe51003014a5902aea9a94521c7bceaa06c42c4308a1a9d06661f63d770c");
        try {
            TenantContextHolder.setTenantId("isolated-tenant");
            DeviceMediaUploadResponse grant = service.issueUploadUrl(request);
            assertTrue(grant.getUploadUrl().startsWith(endpoint));
            HttpURLConnection put = (HttpURLConnection) new URL(grant.getUploadUrl()).openConnection();
            put.setRequestMethod("PUT"); put.setRequestProperty("Content-Type", grant.getRequiredContentType());
            put.setDoOutput(true); put.setFixedLengthStreamingMode(bytes.length);
            try (java.io.OutputStream output = put.getOutputStream()) { output.write(bytes); }
            assertEquals(200, put.getResponseCode()); put.disconnect();
            assertEquals("BOUND", service.validateAndBind(grant.getMediaId(), request.getDeviceId(), grant.getObjectKey(),
                request.getSha256(), "synthetic-message").getUploadStatus());
            // Reusing the still-valid device PUT URL must not mutate bound event evidence.
            HttpURLConnection overwrite = (HttpURLConnection) new URL(grant.getUploadUrl()).openConnection();
            overwrite.setRequestMethod("PUT"); overwrite.setRequestProperty("Content-Type", "video/mp4");
            overwrite.setDoOutput(true); overwrite.setFixedLengthStreamingMode(bytes.length);
            try (java.io.OutputStream output = overwrite.getOutputStream()) { output.write(new byte[bytes.length]); }
            assertEquals(200, overwrite.getResponseCode()); overwrite.disconnect();
            HttpURLConnection get = (HttpURLConnection) new URL(service.getPrivateViewUrl(grant.getMediaId(), 300)).openConnection();
            assertEquals(200, get.getResponseCode()); assertEquals("video/mp4", get.getContentType());
            try (java.io.InputStream input = get.getInputStream()) {
                assertArrayEquals(bytes, org.springframework.util.StreamUtils.copyToByteArray(input));
            } finally { get.disconnect(); }
            HttpURLConnection download = (HttpURLConnection) new URL(service.getPrivateViewUrl(grant.getMediaId(), 300, true)).openConnection();
            assertEquals(200, download.getResponseCode());
            assertEquals("attachment", download.getHeaderField("Content-Disposition")); download.disconnect();
        } finally { TenantContextHolder.clear(); }
    }
}
