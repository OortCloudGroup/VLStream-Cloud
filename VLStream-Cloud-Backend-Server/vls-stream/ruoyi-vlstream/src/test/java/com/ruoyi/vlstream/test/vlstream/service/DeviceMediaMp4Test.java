package com.ruoyi.vlstream.test.vlstream.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceMediaUploadMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadResponse;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real H.264 MP4 fixture; synthetic device/mapper/object bytes only, no external services. */
@Tag("dev")
class DeviceMediaMp4Test {
    private DeviceMediaUploadService service;
    private VlsDeviceMediaProperties properties;
    private VlsDeviceMediaUploadMapper mapper;
    private OssClient client;
    private DeviceMediaUpload upload;
    private byte[] mp4;

    @BeforeEach void setup() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "mp4"), DeviceMediaUpload.class);
        mp4 = Files.readAllBytes(Paths.get(getClass().getResource("/event-media.mp4").toURI()));
        properties = new VlsDeviceMediaProperties();
        properties.setAllowUnauthenticated(true);
        mapper = mock(VlsDeviceMediaUploadMapper.class);
        client = mock(OssClient.class);
        service = spy(new DeviceMediaUploadService());
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "uploadMapper", mapper);
        upload = new DeviceMediaUpload();
        upload.setTenantId("tenant-a"); upload.setDeviceId("SYNTHETIC-CAM"); upload.setMediaId("mp4");
        upload.setObjectKey("events/SYNTHETIC-CAM/mp4.mp4"); upload.setContentType("video/mp4");
        upload.setFileSize((long) mp4.length); upload.setSha256(hash(mp4)); upload.setUploadStatus("ISSUED");
        when(mapper.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<DeviceMediaUpload> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().contains("tenant_id ="));
            assertTrue(query.getParamNameValuePairs().containsValue("tenant-a"));
            return upload;
        });
        when(mapper.update(isNull(), any())).thenReturn(1);
        doReturn(client).when(service).storedClient(any());
        doReturn(client).when(service).ossClient();
        when(client.doesObjectExist(upload.getObjectKey())).thenReturn(true);
        metadata("video/mp4", mp4.length);
        when(client.getObjectContent(anyString())).thenAnswer(invocation -> new ByteArrayInputStream(mp4));
        TenantContextHolder.setTenantId("tenant-a");
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void issuesMp4GrantForResolvedDeviceTenantAndKeepsImageLimitSeparate() {
        WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
        DeviceInfo device = new DeviceInfo(); device.setTenantId("tenant-a");
        when(resolver.resolve("SYNTHETIC-CAM")).thenReturn(device);
        ReflectionTestUtils.setField(service, "wvpDeviceResolver", resolver);
        when(client.getConfigKey()).thenReturn("synthetic");
        doReturn("https://example.invalid/put").when(service).generateUploadUrl(eq(client), anyString(), eq("video/mp4"), eq(600));
        DeviceMediaUploadResponse response = service.issueUploadUrl(request("video/mp4", mp4.length, upload.getSha256()));
        assertEquals("video/mp4", response.getRequiredContentType());
        assertTrue(response.getObjectKey().endsWith(".mp4"));
        verify(mapper).insert(argThat(item -> "tenant-a".equals(item.getTenantId()) && item.getFileSize() == mp4.length));
        assertEquals("tenant-a", TenantContextHolder.getTenantId());
    }

    @Test void bindsRealMp4OnlyAfterByteHashMimeAndContainerValidation() {
        assertEquals("dfd3fe51003014a5902aea9a94521c7bceaa06c42c4308a1a9d06661f63d770c", upload.getSha256());
        DeviceMediaUpload result = bind("SYNTHETIC-CAM", upload.getSha256());
        assertEquals("BOUND", result.getUploadStatus());
        assertEquals("message-1", result.getBoundEventMessageId());
        verify(mapper).update(isNull(), argThat(query -> query.getSqlSegment().contains("tenant_id =")));
    }

    @Test void rejectsUnsupportedMimeOversizeAndPlaceholderHashBeforeIssuing() {
        assertThrows(ServiceException.class, () -> service.issueUploadUrl(request("video/quicktime", 100, upload.getSha256())));
        assertThrows(ServiceException.class, () -> service.issueUploadUrl(request("video/mp4", 100 * 1024 * 1024 + 1L, upload.getSha256())));
        assertThrows(ServiceException.class, () -> service.issueUploadUrl(request("image/jpeg", 10 * 1024 * 1024 + 1L, upload.getSha256())));
        assertThrows(ServiceException.class, () -> service.issueUploadUrl(request("video/mp4", mp4.length, "替换为该MP4文件实际的64位SHA256")));
        verifyNoInteractions(mapper, client);
    }

    @Test void keepsAllThreeRealImageFormatsCompatible() throws Exception {
        for (String extension : new String[] {"jpg", "png", "webp"}) {
            byte[] image = Files.readAllBytes(Paths.get(getClass().getResource("/event-image." + extension).toURI()));
            upload.setContentType("image/" + ("jpg".equals(extension) ? "jpeg" : extension));
            upload.setFileSize((long) image.length); upload.setSha256(hash(image)); upload.setUploadStatus("ISSUED");
            metadata(upload.getContentType(), image.length);
            when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(image));
            assertEquals("BOUND", bind("SYNTHETIC-CAM", upload.getSha256()).getUploadStatus());
        }
        verify(client, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test void rejectsObjectMetadataMimeMismatch() {
        metadata("image/jpeg", mp4.length);
        assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", upload.getSha256()));
        verify(client, never()).getObjectContent(anyString());
        verify(mapper, never()).update(any(), any());
    }

    @Test void rejectsObjectHashMismatch() {
        byte[] changed = mp4.clone(); changed[changed.length - 1] ^= 1;
        when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(changed));
        assertTrue(assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", upload.getSha256())).getMessage().contains("SHA-256"));
        verify(mapper, never()).update(any(), any());
    }

    @Test void rejectsFakeMp4WithCorrectHash() throws Exception {
        byte[] fake = new byte[mp4.length];
        upload.setSha256(hash(fake));
        when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(fake));
        assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", upload.getSha256()));
        verify(mapper, never()).update(any(), any());
    }

    @Test void rejectsTruncatedMp4WithCorrectHash() throws Exception {
        byte[] truncated = java.util.Arrays.copyOf(mp4, mp4.length - 20);
        upload.setFileSize((long) truncated.length); upload.setSha256(hash(truncated)); metadata("video/mp4", truncated.length);
        when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(truncated));
        assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", upload.getSha256()));
    }

    @Test void checksStreamLengthEvenWhenMetadataClaimsExpectedSize() {
        when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(new byte[mp4.length + 1]));
        assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", upload.getSha256()));
        verify(mapper, never()).update(any(), any());
    }

    @Test void rejectsCrossDeviceAndCrossTenantBeforeReadingStorage() {
        assertThrows(ServiceException.class, () -> bind("OTHER-CAM", upload.getSha256()));
        upload.setTenantId("tenant-b");
        assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", upload.getSha256()));
        verifyNoInteractions(client);
        verify(mapper, never()).update(any(), any());
    }

    @Test void deniesMissingTenantAndVideoImageReview() {
        upload.setUploadStatus("BOUND");
        assertThrows(ServiceException.class, () -> service.readBoundImage("mp4", 1024));
        TenantContextHolder.clear();
        assertNull(service.getByMediaId("mp4"));
        verifyNoInteractions(client);
    }

    @Test void boundRetryRequiresSameHashAndEvent() {
        upload.setUploadStatus("BOUND"); upload.setBoundEventMessageId("message-1");
        assertSame(upload, bind("SYNTHETIC-CAM", upload.getSha256()));
        assertThrows(ServiceException.class, () -> bind("SYNTHETIC-CAM", String.join("", Collections.nCopies(64, "0"))));
        assertThrows(ServiceException.class, () -> service.validateAndBind("mp4", "SYNTHETIC-CAM", upload.getObjectKey(), upload.getSha256(), "other-message"));
        verifyNoInteractions(client);
    }

    private DeviceMediaUpload bind(String device, String hash) {
        return service.validateAndBind("mp4", device, upload.getObjectKey(), hash, "message-1");
    }
    private void metadata(String type, long size) {
        ObjectMetadata metadata = new ObjectMetadata(); metadata.setContentType(type); metadata.setContentLength(size);
        when(client.getObjectMetadata(anyString())).thenReturn(metadata);
    }
    private DeviceMediaUploadRequest request(String type, long size, String sha) {
        DeviceMediaUploadRequest request = new DeviceMediaUploadRequest(); request.setDeviceId("SYNTHETIC-CAM");
        request.setFileName("event.mp4"); request.setContentType(type); request.setFileSize(size); request.setSha256(sha);
        return request;
    }
    private static String hash(byte[] bytes) throws Exception {
        StringBuilder value = new StringBuilder();
        for (byte item : MessageDigest.getInstance("SHA-256").digest(bytes)) value.append(String.format("%02x", item & 255));
        return value.toString();
    }
}
