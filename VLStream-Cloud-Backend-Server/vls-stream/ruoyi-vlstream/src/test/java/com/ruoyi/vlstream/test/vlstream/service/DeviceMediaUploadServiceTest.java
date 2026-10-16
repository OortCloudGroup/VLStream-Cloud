/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceMediaUploadMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class DeviceMediaUploadServiceTest {

    @Test
    void batchesDistinctTenantMediaAndIsolatesMissingUnboundAndSigningFailures() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "synthetic"), DeviceMediaUpload.class);
        VlsDeviceMediaUploadMapper mapper = mock(VlsDeviceMediaUploadMapper.class);
        DeviceMediaUpload bound = upload("bound", "tenant-a", "BOUND");
        DeviceMediaUpload broken = upload("broken", "tenant-a", "BOUND");
        DeviceMediaUpload issued = upload("issued", "tenant-a", "ISSUED");
        DeviceMediaUpload foreign = upload("foreign", "tenant-b", "BOUND");
        when(mapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<DeviceMediaUpload> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("tenant_id ="));
            assertTrue(sql.contains("media_id IN"));
            assertEquals(6, query.getParamNameValuePairs().size()); // tenant and five unique IDs
            assertTrue(query.getParamNameValuePairs().containsValue("tenant-a"));
            return Arrays.asList(bound, broken, issued, foreign);
        });
        DeviceMediaUploadService service = spy(new DeviceMediaUploadService());
        setField(service, "uploadMapper", mapper);
        doReturn("https://example.invalid/signed.jpg").when(service).signViewUrl(bound, 3600);
        doThrow(new IllegalStateException("synthetic signing failure")).when(service).signViewUrl(broken, 3600);

        Map<String, String> urls = service.getPrivateViewUrls("tenant-a",
            Arrays.asList("bound", "bound", "broken", "issued", "foreign", "missing", ""), 99999);

        assertEquals(Collections.singletonMap("bound", "https://example.invalid/signed.jpg"), urls);
        verify(mapper, times(1)).selectList(any());
        verify(mapper, never()).selectOne(any());
        verify(service, never()).signViewUrl(eq(issued), anyInt());
        verify(service, never()).signViewUrl(eq(foreign), anyInt());
    }

    @Test
    void emptyPageOrMissingTenantDoesNotQueryMedia() throws Exception {
        VlsDeviceMediaUploadMapper mapper = mock(VlsDeviceMediaUploadMapper.class);
        DeviceMediaUploadService service = new DeviceMediaUploadService();
        setField(service, "uploadMapper", mapper);
        assertTrue(service.getPrivateViewUrls("tenant-a", Collections.emptyList(), 300).isEmpty());
        assertTrue(service.getPrivateViewUrls("", Collections.singletonList("media-1"), 300).isEmpty());
        verifyNoInteractions(mapper);
    }

    private static DeviceMediaUpload upload(String id, String tenant, String status) {
        DeviceMediaUpload upload = new DeviceMediaUpload();
        upload.setMediaId(id);
        upload.setTenantId(tenant);
        upload.setUploadStatus(status);
        return upload;
    }

	@Test
	void signsUploadAndViewUrlsWithConfiguredPublicEndpoint() throws Exception {
		VlsDeviceMediaProperties properties = new VlsDeviceMediaProperties();
		properties.setPublicEndpoint("https://vlstream.example.com:2443");
		OssClient client = mock(OssClient.class);
		when(client.getPresignedPutUrl("events/device/image.jpg", "image/jpeg", 600,
			"https://vlstream.example.com:2443")).thenReturn("https://upload.example");
		when(client.getPrivateUrl("events/device/image.jpg", 300,
			"https://vlstream.example.com:2443")).thenReturn("https://view.example");

		DeviceMediaUploadService service = new DeviceMediaUploadService();
		setField(service, "properties", properties);

		assertEquals("https://upload.example",
			service.generateUploadUrl(client, "events/device/image.jpg", "image/jpeg", 600));
		assertEquals("https://view.example",
			service.generatePrivateViewUrl(client, "events/device/image.jpg", 300));
		verify(client).getPresignedPutUrl("events/device/image.jpg", "image/jpeg", 600,
			"https://vlstream.example.com:2443");
		verify(client).getPrivateUrl("events/device/image.jpg", 300,
			"https://vlstream.example.com:2443");
	}

	@Test
	void rejectsUploadWhenDeviceIsMissingFromWvp() throws Exception {
		VlsDeviceMediaProperties properties = new VlsDeviceMediaProperties();
		properties.setAllowUnauthenticated(true);
		WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
		when(resolver.resolve("UNKNOWN")).thenThrow(new ServiceException("WVP不存在该VLStream设备：UNKNOWN"));

		DeviceMediaUploadService service = new DeviceMediaUploadService();
		setField(service, "properties", properties);
		setField(service, "wvpDeviceResolver", resolver);

		DeviceMediaUploadRequest request = new DeviceMediaUploadRequest();
		request.setDeviceId("UNKNOWN");
		request.setFileName("capture.jpg");
		request.setContentType("image/jpeg");
		request.setFileSize(1024L);
		request.setSha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.issueUploadUrl(request));

		assertEquals("WVP不存在该VLStream设备：UNKNOWN", exception.getMessage());
		verify(resolver).resolve("UNKNOWN");
	}

	private static void setField(Object target, String name, Object value) throws Exception {
        Field field = DeviceMediaUploadService.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
