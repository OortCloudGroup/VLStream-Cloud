package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.compat.SingleTenant;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.DeviceFirmwareMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceFirmwareView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDownloadUrl;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUploadGrant;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareUploadRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceFirmware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DeviceFirmwareServiceTest {

	private DeviceFirmwareMapper mapper;
	private FirmwareObjectStorage storage;
	private DeviceFirmwareService service;

	@BeforeEach
	void setUp() {
		mapper = mock(DeviceFirmwareMapper.class);
		storage = mock(FirmwareObjectStorage.class);
		VlsFirmwareProperties properties = new VlsFirmwareProperties();
		properties.setMaxPackageBytes(160L * 1024L * 1024L);
		properties.setUploadUrlTtlSeconds(600);
		properties.setDownloadUrlTtlSeconds(1800);
		service = new DeviceFirmwareService(mapper, storage, properties);
	}

	@Test
	void issuesSingleObjectUploadGrantForValidFirmware() {
		when(storage.configKey()).thenReturn("minio");
		when(storage.presignedPutUrl(anyString(), eq("application/octet-stream"), eq(600)))
			.thenReturn("http://minio.example/upload");
		when(mapper.insert(any(DeviceFirmware.class))).thenAnswer(invocation -> {
			DeviceFirmware firmware = invocation.getArgument(0);
			firmware.setId(91L);
			return 1;
		});

		FirmwareUploadGrant grant = service.issueUpload(validRequest());

		assertEquals(91L, grant.getFirmwareId());
		assertEquals("http://minio.example/upload", grant.getUploadUrl());
		assertEquals("application/octet-stream", grant.getRequiredContentType());
		verify(mapper).insert(any(DeviceFirmware.class));
	}

	@Test
	void rejectsInvalidVersionAndUnsupportedPackage() {
		FirmwareUploadRequest invalidVersion = validRequest();
		invalidVersion.setFirmwareVersion("v1.2");
		assertThrows(ServiceException.class, () -> service.issueUpload(invalidVersion));

		FirmwareUploadRequest invalidPackage = validRequest();
		invalidPackage.setFileName("firmware.exe");
		assertThrows(ServiceException.class, () -> service.issueUpload(invalidPackage));
	}

	@Test
	void completesUploadOnlyAfterMinioSizeAndShaVerification() {
		DeviceFirmware firmware = readyFixture("UPLOADING");
		when(mapper.selectOne(any())).thenReturn(firmware);
		when(storage.verifyAndCalculateSha256("minio", firmware.getObjectKey(), firmware.getFileSize()))
			.thenReturn(sha256());

		DeviceFirmwareView completed = service.completeUpload(firmware.getId());

		assertEquals("READY", completed.getUploadStatus());
		assertEquals(sha256(), completed.getSha256());
		verify(mapper).updateById(firmware);
	}

	@Test
	void returnsTemporaryDownloadUrlOnlyForReadyFirmware() {
		DeviceFirmware firmware = readyFixture("READY");
		when(mapper.selectOne(any())).thenReturn(firmware);
		when(storage.presignedGetUrl("minio", firmware.getObjectKey(), 1800))
			.thenReturn("http://minio.example/download");

		FirmwareDownloadUrl download = service.downloadUrl(firmware.getId());

		assertEquals("http://minio.example/download", download.getUrl());
		assertEquals("camera-1.0.1.14.ota", download.getFileName());
	}

	@Test
	void deletesMinioObjectBeforeRemovingDatabaseRecord() {
		DeviceFirmware firmware = readyFixture("READY");
		when(mapper.selectOne(any())).thenReturn(firmware);
		when(mapper.deletePermanently(firmware.getId(), SingleTenant.DEFAULT_TENANT_ID)).thenReturn(1);

		service.delete(firmware.getId());

		verify(storage).deleteIfExists("minio", firmware.getObjectKey());
		verify(mapper).deletePermanently(firmware.getId(), SingleTenant.DEFAULT_TENANT_ID);
	}

	private FirmwareUploadRequest validRequest() {
		FirmwareUploadRequest request = new FirmwareUploadRequest();
		request.setCameraModel("IPC-A100");
		request.setTarget("application");
		request.setFirmwareVersion("1.0.1.14");
		request.setFileName("camera-1.0.1.14.ota");
		request.setContentType("application/octet-stream");
		request.setFileSize(1024L);
		return request;
	}

	private DeviceFirmware readyFixture(String uploadStatus) {
		DeviceFirmware firmware = new DeviceFirmware();
		firmware.setId(91L);
		firmware.setTenantId(SingleTenant.DEFAULT_TENANT_ID);
		firmware.setCameraModel("IPC-A100");
		firmware.setTarget("application");
		firmware.setFirmwareVersion("1.0.1.14");
		firmware.setOssConfigKey("minio");
		firmware.setObjectKey("firmware/IPC-A100/application/1.0.1.14/camera.ota");
		firmware.setOriginalFileName("camera-1.0.1.14.ota");
		firmware.setFileSize(1024L);
		firmware.setUploadStatus(uploadStatus);
		if ("READY".equals(uploadStatus)) {
			firmware.setSha256(sha256());
		}
		firmware.setCreateTime(new Date());
		firmware.setIsDeleted(0);
		return firmware;
	}

	private String sha256() {
		return "b84bf7c0392a8dcabd8da974590997778eefc19baea824480883aea9384c3639";
	}
}
