/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmAnnotationService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.RemoteTrainingService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springblade.core.tool.api.R;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAlgorithmTrainingControllerTest {

	@Test
	void completedStatusWaitsUntilPtPathHasBeenPersisted() throws Exception {
		IVlsAlgorithmTrainingService trainingService = mock(IVlsAlgorithmTrainingService.class);
		RemoteTrainingService remoteTrainingService = mock(RemoteTrainingService.class);
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(30L);
		training.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
		training.setProgress(100);
		when(trainingService.selectAlgorithmTrainingById(30L)).thenReturn(training);

		VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
		setField(controller, "vlsAlgorithmTrainingService", trainingService);
		setField(controller, "remoteTrainingService", remoteTrainingService);

		R<RemoteTrainingService.TrainingProgress> response = controller.getTrainingStatus(30L, null);
		RemoteTrainingService.TrainingProgress progress = response.getData();

		assertEquals(AlgorithmTrainingStatusEnum.training.getCode(), progress.getStatus());
		assertEquals(99, progress.getPercentage());
		assertFalse(progress.isCompleted());
		assertEquals("训练完成，正在整理PT模型文件", progress.getMessage());
		verify(remoteTrainingService, never()).getProgress(anyLong(), isNull());
	}

	@Test
	void detectedCompletionIsHeldUntilPtPathHasBeenPersisted() throws Exception {
		IVlsAlgorithmTrainingService trainingService = mock(IVlsAlgorithmTrainingService.class);
		RemoteTrainingService remoteTrainingService = mock(RemoteTrainingService.class);
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(31L);
		training.setTrainStatus(AlgorithmTrainingStatusEnum.training);
		RemoteTrainingService.TrainingProgress detected = new RemoteTrainingService.TrainingProgress();
		detected.setTaskId(31L);
		detected.setCompleted(true);
		detected.setStatus(AlgorithmTrainingStatusEnum.completed.getCode());
		detected.setPercentage(100);
		when(trainingService.selectAlgorithmTrainingById(31L)).thenReturn(training);
		when(remoteTrainingService.getProgress(31L, null)).thenReturn(detected);

		VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
		setField(controller, "vlsAlgorithmTrainingService", trainingService);
		setField(controller, "remoteTrainingService", remoteTrainingService);

		R<RemoteTrainingService.TrainingProgress> response = controller.getTrainingStatus(31L, null);
		RemoteTrainingService.TrainingProgress progress = response.getData();

		assertEquals(AlgorithmTrainingStatusEnum.training.getCode(), progress.getStatus());
		assertEquals(99, progress.getPercentage());
		assertFalse(progress.isCompleted());
		verify(trainingService, never()).updateAlgorithmTraining(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void startPersistsTheExactSelectedDatasetIdBeforeEnqueueing() throws Exception {
		IVlsAlgorithmTrainingService trainingService = mock(IVlsAlgorithmTrainingService.class);
		IVlsAlgorithmAnnotationService annotationService = mock(IVlsAlgorithmAnnotationService.class);
		IVlsAlgorithmService algorithmService = mock(IVlsAlgorithmService.class);
		GpuTrainingSchedulerService schedulerService = mock(GpuTrainingSchedulerService.class);

		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(40L);
		training.setAlgorithmId(50L);
		AlgorithmAnnotation annotation = new AlgorithmAnnotation();
		annotation.setDatasetPath("/data/datasets/annotation-60/dataset.yaml");
		Algorithm algorithm = new Algorithm();
		algorithm.setCategory(AlgorithmCategoryEnum.detect);
		algorithm.setPtModelFilePath("/data/models/yolov8m.pt");
		RemoteTrainingService.StartResult startResult = new RemoteTrainingService.StartResult();
		startResult.setLogPath("/data/logs/training-40.log");

		when(trainingService.selectAlgorithmTrainingById(40L)).thenReturn(training);
		when(trainingService.updateAlgorithmTraining(org.mockito.ArgumentMatchers.any())).thenReturn(1);
		when(annotationService.getById(60L)).thenReturn(annotation);
		when(algorithmService.getById(50L)).thenReturn(algorithm);
		when(schedulerService.enqueue(anyString(), org.mockito.ArgumentMatchers.eq(40L), anyString(),
			anyString(), org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(16),
			org.mockito.ArgumentMatchers.eq(640))).thenReturn(startResult);

		VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
		setField(controller, "vlsAlgorithmTrainingService", trainingService);
		setField(controller, "algorithmAnnotationService", annotationService);
		setField(controller, "algorithmService", algorithmService);
		setField(controller, "gpuTrainingSchedulerService", schedulerService);

		controller.startTraining(40L, 10, 60L, 16, 640, null);

		ArgumentCaptor<AlgorithmTraining> updates = ArgumentCaptor.forClass(AlgorithmTraining.class);
		verify(trainingService, org.mockito.Mockito.times(2)).updateAlgorithmTraining(updates.capture());
		assertEquals(60L, updates.getAllValues().get(0).getDatasetId());
	}

	@Test
	void convertsFormatsInOrderToAvoidSharedOnnxIntermediateFiles() throws Exception {
		IVlsAlgorithmTrainingService trainingService = mock(IVlsAlgorithmTrainingService.class);
		IVlsAlgorithmAnnotationService annotationService = mock(IVlsAlgorithmAnnotationService.class);
		RemoteTrainingService remoteTrainingService = mock(RemoteTrainingService.class);
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(70L);
		training.setDatasetId(71L);
		training.setModelOutputPath("/data/weights/测试.pt");
		AlgorithmAnnotation annotation = new AlgorithmAnnotation();
		annotation.setDatasetPath("/data/datasets/annotation-71/dataset.yaml");
		List<String> calls = new CopyOnWriteArrayList<>();
		CountDownLatch completed = new CountDownLatch(1);

		when(trainingService.selectAlgorithmTrainingById(70L)).thenReturn(training);
		when(trainingService.updateAlgorithmTraining(org.mockito.ArgumentMatchers.any())).thenReturn(1);
		when(annotationService.getById(71L)).thenReturn(annotation);
		when(remoteTrainingService.exportModel("/data/weights/测试.pt", "onnx")).thenAnswer(invocation -> {
			calls.add("onnx");
			return null;
		});
		when(remoteTrainingService.exportHisiliconOm("/data/weights/测试.pt",
			"/data/datasets/annotation-71/dataset.yaml")).thenAnswer(invocation -> {
			calls.add("om");
			return "/data/weights/测试.om";
		});
		when(remoteTrainingService.exportModel("/data/weights/测试.pt", "rknn")).thenAnswer(invocation -> {
			calls.add("rknn");
			completed.countDown();
			return "/data/weights/测试-rk3588.rknn";
		});

		VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
		setField(controller, "vlsAlgorithmTrainingService", trainingService);
		setField(controller, "algorithmAnnotationService", annotationService);
		setField(controller, "remoteTrainingService", remoteTrainingService);

		controller.convertModel(70L);

		assertTrue(completed.await(2, TimeUnit.SECONDS));
		assertEquals(Arrays.asList("onnx", "om", "rknn"), calls);
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
