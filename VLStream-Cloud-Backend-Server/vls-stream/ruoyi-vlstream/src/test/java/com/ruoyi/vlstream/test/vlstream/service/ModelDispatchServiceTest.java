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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
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
	private ModelClassFileService classFileService;
    private IVlsAlgorithmModelService modelService;

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
        modelService = mock(IVlsAlgorithmModelService.class);
        setField(service, "modelService", modelService);
		classFileService = mock(ModelClassFileService.class);
		setField(service, "classFileService", classFileService);
		when(classFileService.prepare(any())).thenReturn(new ModelClassFileService.ClassFile(
			"dataset.yaml", "names: [person]\n", 16L, repeat("b", 64)));
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
		org.mockito.ArgumentCaptor<java.util.Map> envelope = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
		verify(mqttService).publish(eq("vlstream/v2.2/dev/CAM-A/bus"), envelope.capture());
		java.util.Map payload = (java.util.Map) envelope.getValue().get("payload");
		assertEquals("dataset.yaml", payload.get("classFileName"));
		assertEquals(repeat("b", 64), payload.get("classFileSha256"));
		org.junit.jupiter.api.Assertions.assertTrue(payload.get("classFileUrl").toString().contains("/classes?"));
		org.mockito.ArgumentCaptor<ModelDispatchTask> tasks = org.mockito.ArgumentCaptor.forClass(ModelDispatchTask.class);
		verify(taskService, org.mockito.Mockito.times(2)).create(tasks.capture());
		assertEquals("names: [person]\n", tasks.getAllValues().get(0).getClassFileContent());
	}

	@Test
	void missingClassFileDoesNotPublishOrFallBackToAnotherModel() throws Exception {
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(10L);
		when(trainingService.list(any())).thenReturn(Collections.singletonList(training));
		when(artifactService.resolvePath(training, "om")).thenReturn("/data/work/model.om");
		when(artifactService.inspect("/data/work/model.om"))
			.thenReturn(new RemoteModelArtifactService.ArtifactMetadata("model.om", 10L, repeat("a", 64)));
		properties.setSigningSecret("test-signing-secret");
		when(classFileService.prepare(training)).thenThrow(new java.io.IOException("类别 YAML 缺失"));
		ServiceException ex = assertThrows(ServiceException.class, () -> service.dispatch(1L, "CAM-A", "om"));
		assertEquals("模型对应的类别文件不可用：类别 YAML 缺失", ex.getMessage());
		org.mockito.Mockito.verifyNoInteractions(mqttService, taskService, wvpDeviceResolver);
	}

	private DeviceInfo wvpDevice(Long id, String deviceId) {
		DeviceInfo device = new DeviceInfo();
		device.setId(id);
		device.setDeviceId(deviceId);
		device.setDeviceName(deviceId);
		return device;
	}

    @Test
    void selectedVersionUsesSavedPathsWithoutLookingUpLatestTraining() throws Exception {
        AlgorithmModel model = new AlgorithmModel();
        model.setId(9001L); model.setAlgorithmId(1L); model.setTrainingId(10L); model.setTenantId("000000");
        model.setModelPath("/old/weights/best.pt"); model.setOmModelOutputPath("/old/weights/best.om");
        when(modelService.getById(9001L)).thenReturn(model);
        properties.setSigningSecret("test-signing-secret");
        when(wvpDeviceResolver.resolveOnline("CAM-A")).thenReturn(wvpDevice(101L, "CAM-A"));
        when(artifactService.resolvePath(any(AlgorithmTraining.class), eq("om"))).thenAnswer(call ->
            ((AlgorithmTraining) call.getArgument(0)).getOmModelOutputPath());
        when(artifactService.inspect("/old/weights/best.om")).thenReturn(
            new RemoteModelArtifactService.ArtifactMetadata("best.om", 10L, repeat("a", 64)));
        when(signatureService.sign(any(), anyLong())).thenReturn("signature");

        String requestId = service.dispatchModel(9001L, "CAM-A");

        org.junit.jupiter.api.Assertions.assertNotNull(requestId);
        org.mockito.Mockito.verifyNoInteractions(trainingService);
        org.mockito.ArgumentCaptor<AlgorithmTraining> snapshot = org.mockito.ArgumentCaptor.forClass(AlgorithmTraining.class);
        verify(classFileService).prepare(snapshot.capture());
        assertEquals("/old/weights/best.pt", snapshot.getValue().getModelOutputPath());
        assertEquals("000000", snapshot.getValue().getTenantId());
        org.mockito.ArgumentCaptor<ModelDispatchTask> task = org.mockito.ArgumentCaptor.forClass(ModelDispatchTask.class);
        verify(taskService).create(task.capture());
        assertEquals("/old/weights/best.om", task.getValue().getRemotePath());
        assertEquals(requestId, task.getValue().getRequestId());
    }

    @Test
    void missingSelectedModelOrOmNeverFallsBackOrPublishes() {
        assertThrows(ServiceException.class, () -> service.dispatchModel(9001L, "CAM-A"));
        AlgorithmModel model = new AlgorithmModel();
        model.setTrainingId(10L); model.setAlgorithmId(1L); model.setModelPath("/old/weights/best.pt");
        when(modelService.getById(9001L)).thenReturn(model);
        assertThrows(ServiceException.class, () -> service.dispatchModel(9001L, "CAM-A"));
        org.mockito.Mockito.verifyNoInteractions(trainingService, mqttService, wvpDeviceResolver);
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
