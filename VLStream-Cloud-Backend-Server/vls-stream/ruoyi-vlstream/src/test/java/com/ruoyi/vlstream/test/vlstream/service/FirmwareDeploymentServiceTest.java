package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.MqttDeviceDetailView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwarePackageDownload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceFirmware;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.FirmwareDeployTask;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class FirmwareDeploymentServiceTest {

	private MqttDeviceMapper deviceMapper;
	private DeviceFirmwareService firmwareService;
	private FirmwareDeployTaskService taskService;
	private VlsMqttBusService mqttService;
	private FirmwareDeploymentService service;
	private MqttDevice device;
	private DeviceFirmware firmware;

	@BeforeEach
	void setUp() {
		deviceMapper = mock(MqttDeviceMapper.class);
		firmwareService = mock(DeviceFirmwareService.class);
		taskService = mock(FirmwareDeployTaskService.class);
		mqttService = mock(VlsMqttBusService.class);
		VlsFirmwareProperties properties = new VlsFirmwareProperties();
		properties.setPlatformBaseUrl("http://192.168.88.31:8080");
		service = new FirmwareDeploymentService(deviceMapper, firmwareService, taskService, mqttService, properties);
		device = new MqttDevice();
		device.setId(10L);
		device.setDeviceId("CAM-1");
		device.setDeviceModel("OORT-6600-2.5");
		device.setApplicationVersion("1.0.1.14");
		device.setOnline(true);
		firmware = new DeviceFirmware();
		firmware.setId(20L);
		firmware.setCameraModel(device.getDeviceModel());
		firmware.setTarget("application");
		firmware.setFirmwareVersion("1.0.1.15");
		firmware.setOriginalFileName("app-1.0.1.15.ota");
		firmware.setFileSize(1024L);
		firmware.setSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		when(deviceMapper.selectOne(any())).thenReturn(device);
		when(firmwareService.findLatestReady(device.getDeviceModel(), "application")).thenReturn(firmware);
	}

	@Test
	void detailEnablesUpgradeOnlyForHigherCompatibleVersion() {
		MqttDeviceDetailView detail = service.detail(device.getId());

		assertTrue(detail.isHasNewFirmware());
		assertTrue(detail.isCanUpgrade());
		assertEquals("1.0.1.15", detail.getAvailableUpgrades().get(0).getLatestVersion());
	}

	@Test
	@SuppressWarnings("unchecked")
	void persistsTaskBeforePublishingCompleteEnvelope() {
		when(firmwareService.requiredReady(firmware.getId())).thenReturn(firmware);
		when(firmwareService.otaDownloadTtlSeconds()).thenReturn(7200);
		ArgumentCaptor<FirmwareDeployTask> taskCaptor = ArgumentCaptor.forClass(FirmwareDeployTask.class);
		ArgumentCaptor<Object> envelopeCaptor = ArgumentCaptor.forClass(Object.class);

		service.deploy(device.getId(), firmware.getId());

		verify(taskService).create(taskCaptor.capture());
		verify(mqttService).publish(eq("vlstream/v2.2/dev/CAM-1/bus"), envelopeCaptor.capture());
		Map<String, Object> envelope = (Map<String, Object>) envelopeCaptor.getValue();
		Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
		assertEquals("firmwareDeploy", envelope.get("subBizType"));
		assertEquals("OORT-6600-2.5", payload.get("deviceModel"));
		assertEquals(firmware.getSha256(), payload.get("sha256"));
		assertTrue(String.valueOf(payload.get("packageUrl")).startsWith(
			"http://192.168.88.31:8080/vlsDeviceFirmware/ota/"));
		assertEquals(Boolean.FALSE, payload.get("rebootAfter"));
		verify(taskService).markPublished(taskCaptor.getValue().getRequestId());
	}

	@Test
	void authorizesShortLinkOnlyForMatchingUnexpiredTask() {
		FirmwareDeployTask task = new FirmwareDeployTask();
		task.setRequestId("request-1");
		task.setMqttMessageId("fw-message-1");
		task.setFirmwareId(firmware.getId());
		task.setFileSize(firmware.getFileSize());
		task.setSha256(firmware.getSha256());
		task.setDownloadExpiresAt(Instant.now().getEpochSecond() + 60);
		FirmwarePackageDownload packageDownload = FirmwarePackageDownload.builder()
			.inputStream(new ByteArrayInputStream(new byte[0]))
			.fileName(firmware.getOriginalFileName())
			.contentType("application/octet-stream")
			.fileSize(firmware.getFileSize())
			.sha256(firmware.getSha256())
			.build();
		when(taskService.getByRequestId("request-1")).thenReturn(task);
		when(firmwareService.openOtaPackage(firmware.getId())).thenReturn(packageDownload);

		assertEquals(packageDownload, service.download("request-1", "fw-message-1"));
		assertThrows(com.ruoyi.common.exception.ServiceException.class,
			() -> service.download("request-1", "wrong-message"));
	}
}
