/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class ModelDispatchServiceTest {

	private IVlsAlgorithmTrainingService trainingService;
	private RemoteModelArtifactService artifactService;
	private VlsModelDispatchProperties properties;
	private WvpVlStreamDeviceResolver wvpDeviceResolver;
	private ModelDispatchTaskService taskService;
	private VlsMqttBusService mqttService;
	private ModelDownloadSignatureService signatureService;
	private ModelDispatchService service;

	@BeforeEach
	void setUp() throws Exception {
		trainingService = mock(IVlsAlgorithmTrainingService.class);
		artifactService = mock(RemoteModelArtifactService.class);
		wvpDeviceResolver = mock(WvpVlStreamDeviceResolver.class);
		taskService = mock(ModelDispatchTaskService.class);
		mqttService = mock(VlsMqttBusService.class);
		signatureService = mock(ModelDownloadSignatureService.class);
		properties = new VlsModelDispatchProperties();
		properties.setPublicBaseUrl("http://192.168.88.31:8080");

		service = new ModelDispatchService();
		setField(service, "trainingService", trainingService);
		setField(service, "artifactService", artifactService);
		setField(service, "wvpDeviceResolver", wvpDeviceResolver);
		setField(service, "taskService", taskService);
		setField(service, "mqttService", mqttService);
		setField(service, "signatureService", signatureService);
		setField(service, "dispatchProperties", properties);

		when(artifactService.normalizeType("om")).thenReturn("om");
	}

	@Test
	void reportsMissingCompletedTrainingInsteadOfGenericDispatchFailure() {
		when(trainingService.list(any())).thenReturn(Collections.emptyList());

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.dispatch(2079813710632751106L, "2081669936341602305", "om"));

		assertEquals("算法 2079813710632751106 没有已完成的训练任务，无法下发 OM 模型",
			exception.getMessage());
	}

	@Test
	void reportsMissingSigningSecretBeforePublishing() throws Exception {
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(1L);
		when(trainingService.list(any())).thenReturn(Collections.singletonList(training));
		when(artifactService.resolvePath(training, "om")).thenReturn("/data/work/model.om");
		when(artifactService.inspect("/data/work/model.om"))
			.thenReturn(new RemoteModelArtifactService.ArtifactMetadata("model.om", 10L, repeat("a", 64)));

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.dispatch(1L, "2081669936341602305", "om"));

		assertEquals("未配置模型下载签名密钥 VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET",
			exception.getMessage());
	}

	@Test
	void resolvesWvpBusinessDeviceIdsAndPublishesWithoutLegacyDeviceRows() throws Exception {
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(10L);
		when(trainingService.list(any())).thenReturn(Collections.singletonList(training));
		when(artifactService.resolvePath(training, "om")).thenReturn("/data/work/model.om");
		when(artifactService.inspect("/data/work/model.om"))
			.thenReturn(new RemoteModelArtifactService.ArtifactMetadata("model.om", 10L, repeat("a", 64)));
		properties.setSigningSecret("test-signing-secret");
		when(signatureService.sign(any(), anyLong())).thenReturn("signature");

		DeviceInfo first = wvpDevice(101L, "CAM-A");
		DeviceInfo second = wvpDevice(102L, "CAM-B");
		when(wvpDeviceResolver.resolve("CAM-A")).thenReturn(first);
		when(wvpDeviceResolver.resolve("CAM-B")).thenReturn(second);

		service.dispatch(1L, "CAM-A,CAM-B", "om");

		verify(wvpDeviceResolver).resolve("CAM-A");
		verify(wvpDeviceResolver).resolve("CAM-B");
		verify(mqttService).publish(eq("vlstream/v2.2/dev/CAM-A/bus"), any());
		verify(mqttService).publish(eq("vlstream/v2.2/dev/CAM-B/bus"), any());
		verify(taskService, org.mockito.Mockito.times(2)).create(any(ModelDispatchTask.class));
		verifyNoMoreInteractions(wvpDeviceResolver);
	}

	private DeviceInfo wvpDevice(Long id, String deviceId) {
		DeviceInfo device = new DeviceInfo();
		device.setId(id);
		device.setDeviceId(deviceId);
		device.setDeviceName(deviceId);
		return device;
	}

	private String repeat(String value, int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			builder.append(value);
		}
		return builder.toString();
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
