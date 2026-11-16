/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmLlmReviewConfigMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class LlmReviewTaskServiceTest {

	@AfterEach
	void clearTenant() { TenantContextHolder.clear(); }

	@Test
	void submitsExistingEventUsingDisabledAlgorithmConfigAndBoundImage() {
		Fixture fixture = new Fixture(true);
		fixture.existingEvent();
		LlmReviewTask task = fixture.service.reviewExistingEvent("camera-20261116-4");
		assertEquals("camera-20261116-4", task.getFormalEventId());
		assertEquals("media-1", task.getMediaId());
		assertEquals("PENDING", task.getReviewStatus());
		assertEquals("检查告警", JSONUtil.parseObj(task.getConfigSnapshotJson()).getStr("promptTemplate"));
		verify(fixture.sink, never()).report(any());
	}

	@Test
	void repeatedSubmissionReturnsExistingTask() {
		Fixture fixture = new Fixture(true);
		fixture.existingEvent();
		LlmReviewTask existing = new LlmReviewTask(); existing.setId(99L);
		when(fixture.taskMapper.selectOne(any())).thenReturn(existing);
		assertEquals(existing, fixture.service.reviewExistingEvent("camera-20261116-4"));
		verify(fixture.taskMapper, never()).insertIgnore(any());
	}

	@Test
	void concurrentSubmissionReturnsTaskFromCurrentRead() {
		Fixture fixture = new Fixture(true);
		fixture.existingEvent();
		LlmReviewTask existing = new LlmReviewTask(); existing.setId(99L);
		when(fixture.taskMapper.insertIgnore(any())).thenReturn(0);
		when(fixture.taskMapper.findExistingForUpdate("000000", "CAM-1", "event-1")).thenReturn(existing);
		assertEquals(existing, fixture.service.reviewExistingEvent("camera-20261116-4"));
	}

	@Test
	void rejectsForeignTenantAndForeignDeviceMedia() {
		Fixture fixture = new Fixture(true);
		JSONObject event = fixture.existingEvent();
		event.set("tenantId", "other");
		assertThrows(ServiceException.class, () -> fixture.service.reviewExistingEvent("camera-20261116-4"));
		event.set("tenantId", "000000");
		DeviceMediaUpload foreign = fixture.upload(); foreign.setTenantId("other"); foreign.setDeviceId("CAM-1");
		when(fixture.media.getByMediaId("media-1")).thenReturn(foreign);
		assertThrows(ServiceException.class, () -> fixture.service.reviewExistingEvent("camera-20261116-4"));
		verify(fixture.taskMapper, never()).insertIgnore(any());
	}

	@Test
	void rejectsMissingImageAndOriginalDeviceMetadata() {
		Fixture fixture = new Fixture(true);
		JSONObject event = fixture.existingEvent();
		when(fixture.media.getByMediaId("media-1")).thenReturn(null);
		assertThrows(ServiceException.class, () -> fixture.service.reviewExistingEvent("camera-20261116-4"));
		event.remove("sourceMessageId");
		assertThrows(ServiceException.class, () -> fixture.service.reviewExistingEvent("camera-20261116-4"));
		verify(fixture.taskMapper, never()).insertIgnore(any());
	}

	@Test
	void completedReviewKeepsExistingFormalEventForBothDecisions() throws Exception {
		for (String verdict : new String[]{"CONFIRMED", "REJECTED"}) {
			Fixture fixture = new Fixture(true); fixture.existingEvent();
			LlmReviewTask task = fixture.service.reviewExistingEvent("camera-20261116-4");
			OpenAiVisionClient.VisionDecision decision = new OpenAiVisionClient.VisionDecision();
			decision.setDecision(verdict); decision.setConfidence(new BigDecimal("0.95"));
			when(fixture.media.readBoundImage(anyString(), anyInt())).thenReturn(new byte[]{1});
			when(fixture.visionClient.review(any(), anyString(), anyList())).thenReturn(decision);
			when(fixture.taskMapper.finishAttempt(any(), anyString(), anyString(), anyString(), anyInt(), any(),
				nullable(String.class), nullable(BigDecimal.class), nullable(String.class), nullable(String.class),
				nullable(String.class), nullable(String.class), any())).thenReturn(1);
			fixture.service.processClaimed(task, "worker");
			verify(fixture.sink, never()).report(any());
			verify(fixture.taskMapper).finishAttempt(eq(task.getId()), eq("000000"), eq("worker"), eq(verdict), eq(1), any(),
				eq(verdict), any(), nullable(String.class), nullable(String.class), nullable(String.class), eq("camera-20261116-4"), any());
		}
	}

	@Test
	void keepsExistingFlowWhenGlobalReviewSwitchIsOff() {
		Fixture fixture = new Fixture(false);

		boolean queued = fixture.service.enqueueIfRequired(fixture.envelope(), fixture.device(),
			fixture.upload(), "event-1", "person_detected", "检测到人员", new Date());

		assertFalse(queued);
		verify(fixture.taskMapper, never()).insertIgnore(any());
	}

	@Test
	void snapshotsEventWhenAlgorithmReviewIsEnabled() {
		Fixture fixture = new Fixture(true);
		AlgorithmLlmReviewConfig config = new AlgorithmLlmReviewConfig();
		config.setProviderId(22L);
		config.setEnabled(true);
		config.setPromptTemplate("检查 {{eventType}}");
		config.setDecisionThreshold(new BigDecimal("0.80"));
		config.setMaxRetries(2);
		config.setFailureStrategy("MANUAL_REVIEW");
		config.setImageMode("FULL_AND_CROP");
		when(fixture.configMapper.selectOne(any())).thenReturn(config);

		boolean queued = fixture.service.enqueueIfRequired(fixture.envelope(), fixture.device(),
			fixture.upload(), "event-1", "person_detected", "检测到人员", new Date(1000L));

		assertTrue(queued);
		ArgumentCaptor<LlmReviewTask> captor = ArgumentCaptor.forClass(LlmReviewTask.class);
		verify(fixture.taskMapper).insertIgnore(captor.capture());
		LlmReviewTask task = captor.getValue();
		assertEquals(Long.valueOf(11L), task.getAlgorithmId());
		assertEquals(Long.valueOf(22L), task.getProviderId());
		assertEquals("PENDING", task.getReviewStatus());
		assertEquals("person_detected", JSONUtil.parseObj(task.getEventPayloadJson()).getStr("eventType"));
	}

	@Test
	void usesSingleImagePromptForProviderConnectivityTest() {
		Fixture fixture = new Fixture(true);

		fixture.service.testProvider(22L, "", new byte[]{1, 2, 3});

		verify(fixture.visionClient).test(eq(fixture.systemProvider),
			eq(LlmReviewTaskService.PROVIDER_TEST_PROMPT), anyList());
	}

	@Test
	void queuesRequiredReviewWhenTenantAuthorizationIsUnavailable() {
		Fixture fixture = new Fixture(true);
		AlgorithmLlmReviewConfig config = new AlgorithmLlmReviewConfig();
		config.setProviderId(22L);
		config.setEnabled(true);
		when(fixture.configMapper.selectOne(any())).thenReturn(config);
		when(fixture.systemProviderService.isAuthorized(fixture.systemProvider)).thenReturn(false);

		boolean queued = fixture.service.enqueueIfRequired(fixture.envelope(), fixture.device(),
			fixture.upload(), "event-1", "person_detected", "检测到人员", new Date());

		assertTrue(queued);
		verify(fixture.taskMapper).insertIgnore(any());
		verify(fixture.sink, never()).report(any());
	}

	@Test
	void automaticReviewCreatesFormalEventOnlyForConfirmedVerdict() throws Exception {
		for (String verdict : new String[]{"CONFIRMED", "REJECTED", "UNCERTAIN"}) {
			Fixture fixture = new Fixture(true);
			AlgorithmLlmReviewConfig config = new AlgorithmLlmReviewConfig();
			config.setProviderId(22L); config.setEnabled(true); config.setImageMode("FULL");
			config.setDecisionThreshold(new BigDecimal("0.80"));
			when(fixture.configMapper.selectOne(any())).thenReturn(config);
			assertTrue(fixture.service.enqueueIfRequired(fixture.envelope(), fixture.device(), fixture.upload(),
				"event-1", "person_detected", "检测到人员", new Date()));
			ArgumentCaptor<LlmReviewTask> created = ArgumentCaptor.forClass(LlmReviewTask.class);
			verify(fixture.taskMapper).insertIgnore(created.capture());
			LlmReviewTask task = created.getValue();
			assertEquals(null, task.getFormalEventId());
			OpenAiVisionClient.VisionDecision decision = new OpenAiVisionClient.VisionDecision();
			decision.setDecision(verdict); decision.setConfidence(new BigDecimal("0.95"));
			when(fixture.media.readBoundImage(anyString(), anyInt())).thenReturn(new byte[]{1});
			when(fixture.visionClient.review(any(), anyString(), anyList())).thenReturn(decision);
			when(fixture.sink.report(any())).thenReturn(com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult
				.builder().activeSafetyEventId("formal-new").build());
			fixture.stubFinish();
			fixture.service.processClaimed(task, "worker");
			if ("CONFIRMED".equals(verdict)) verify(fixture.sink).report(any());
			else verify(fixture.sink, never()).report(any());
			String status = "UNCERTAIN".equals(verdict) ? "MANUAL_REVIEW" : verdict;
			String formalId = "CONFIRMED".equals(verdict) ? "formal-new" : null;
			verify(fixture.taskMapper).finishAttempt(eq(task.getId()), eq("000000"), eq("worker"), eq(status), eq(1), any(),
				eq(verdict), any(), nullable(String.class), nullable(String.class), nullable(String.class), eq(formalId), any());
		}
	}

	@Test
	void unavailableProviderExhaustsRetriesIntoManualReviewInsteadOfFormalEvent() {
		Fixture fixture = new Fixture(true);
		AlgorithmLlmReviewConfig config = new AlgorithmLlmReviewConfig();
		config.setProviderId(22L); config.setEnabled(true); config.setMaxRetries(0);
		when(fixture.configMapper.selectOne(any())).thenReturn(config);
		when(fixture.systemProviderService.isAuthorized(fixture.systemProvider)).thenReturn(false);
		assertTrue(fixture.service.enqueueIfRequired(fixture.envelope(), fixture.device(), fixture.upload(),
			"event-1", "person_detected", "检测到人员", new Date()));
		ArgumentCaptor<LlmReviewTask> created = ArgumentCaptor.forClass(LlmReviewTask.class);
		verify(fixture.taskMapper).insertIgnore(created.capture());
		LlmReviewTask task = created.getValue();
		fixture.stubFinish();
		fixture.service.processClaimed(task, "worker");
		verify(fixture.taskMapper).finishAttempt(eq(task.getId()), eq("000000"), eq("worker"), eq("MANUAL_REVIEW"), eq(1), any(),
			nullable(String.class), nullable(BigDecimal.class), nullable(String.class), nullable(String.class), anyString(), eq(null), any());
		verify(fixture.sink, never()).report(any());
		verify(fixture.visionClient, never()).review(any(), anyString(), anyList());
	}

	@Test
	void doesNotBypassRequiredImageReviewForAnExternalEvent() {
		Fixture fixture = new Fixture(true);
		AlgorithmLlmReviewConfig config = new AlgorithmLlmReviewConfig();
		config.setProviderId(22L);
		config.setEnabled(true);
		when(fixture.configMapper.selectOne(any())).thenReturn(config);

		ServiceException error = assertThrows(ServiceException.class, () -> fixture.service.enqueueIfRequired(
			fixture.envelope(), fixture.device(), null, "event-1", "person_detected", "检测到人员", new Date()));

		assertEquals("当前算法要求图片复核，但媒体属于其他环境，无法在本机读取", error.getMessage());
		verify(fixture.taskMapper, never()).insertIgnore(any());
	}

	private static class Fixture {
		private final AlgorithmLlmReviewConfigMapper configMapper = mock(AlgorithmLlmReviewConfigMapper.class);
		private final LlmReviewTaskMapper taskMapper = mock(LlmReviewTaskMapper.class);
		private final LlmProviderMapper providerMapper = mock(LlmProviderMapper.class);
		private final LlmSystemProviderService systemProviderService = mock(LlmSystemProviderService.class);
		private final OpenAiVisionClient visionClient = mock(OpenAiVisionClient.class);
		private final DeviceMediaUploadService media = mock(DeviceMediaUploadService.class);
		private final ActiveSafetyEventReportService sink = mock(ActiveSafetyEventReportService.class);
		private final LlmReviewManagementService management = mock(LlmReviewManagementService.class);
		private final LlmProvider systemProvider = new LlmProvider();
		private final LlmReviewTaskService service;

		Fixture(boolean enabled) {
			VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
			properties.setEnabled(enabled);
			systemProvider.setId(22L);
			systemProvider.setName(LlmSystemProviderService.SYSTEM_PROVIDER_NAME);
			systemProvider.setEnabled(true);
			systemProvider.setApiKeyCiphertext("ciphertext");
			when(providerMapper.selectById(22L)).thenReturn(systemProvider);
			when(systemProviderService.isSystemProvider(systemProvider)).thenReturn(true);
			when(systemProviderService.isAuthorized(systemProvider)).thenReturn(true);
			when(taskMapper.insertIgnore(any())).thenReturn(1);
			service = new LlmReviewTaskService(properties, configMapper, providerMapper, systemProviderService,
				taskMapper, media, visionClient, sink, management);
		}

		JSONObject existingEvent() {
			TenantContextHolder.setTenantId("000000");
			JSONObject event = new JSONObject().set("tenantId", "000000").set("sourceMessageId", "message-1")
				.set("deviceEventId", "event-1").set("deviceId", "CAM-1").set("mediaId", "media-1")
				.set("eventType", "person_detected").set("mqttPayload", envelope().getJSONObject("payload"));
			when(sink.loadReviewSource("camera-20261116-4", "000000")).thenReturn(event);
			AlgorithmLlmReviewConfig config = new AlgorithmLlmReviewConfig();
			config.setProviderId(22L); config.setEnabled(false); config.setPromptTemplate("检查告警");
			config.setImageMode("FULL_AND_CROP"); config.setDecisionThreshold(new BigDecimal("0.80"));
			when(management.getEventReviewConfig(11L)).thenReturn(config);
			DeviceMediaUpload image = upload(); image.setTenantId("000000"); image.setDeviceId("CAM-1");
			image.setUploadStatus("BOUND"); image.setBoundEventMessageId("message-1"); image.setContentType("image/jpeg");
			when(media.getByMediaId("media-1")).thenReturn(image);
			return event;
		}

		void stubFinish() {
			when(taskMapper.finishAttempt(any(), anyString(), anyString(), anyString(), anyInt(), any(),
				nullable(String.class), nullable(BigDecimal.class), nullable(String.class), nullable(String.class),
				nullable(String.class), nullable(String.class), any())).thenReturn(1);
		}

		JSONObject envelope() {
			return JSONUtil.parseObj("{\"messageId\":\"message-1\",\"subBizType\":\"struct\","
				+ "\"payload\":{\"algorithmId\":\"11\",\"type\":\"human\",\"worth\":0.9}}");
		}

		DeviceInfo device() {
			DeviceInfo device = new DeviceInfo();
			device.setDeviceId("CAM-1");
			device.setDeviceName("摄像机一");
			device.setTenantId("000000");
			return device;
		}

		DeviceMediaUpload upload() {
			DeviceMediaUpload upload = new DeviceMediaUpload();
			upload.setMediaId("media-1");
			return upload;
		}
	}
}
