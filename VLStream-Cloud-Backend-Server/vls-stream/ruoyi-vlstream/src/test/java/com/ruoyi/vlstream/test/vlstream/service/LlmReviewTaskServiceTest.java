/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class LlmReviewTaskServiceTest {

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
		LlmProvider provider = new LlmProvider();
		provider.setId(22L);
		when(fixture.providerMapper.selectById(22L)).thenReturn(provider);

		fixture.service.testProvider(22L, "", new byte[]{1, 2, 3});

		verify(fixture.visionClient).review(eq(provider),
			eq(LlmReviewTaskService.PROVIDER_TEST_PROMPT), anyList());
	}

	private static class Fixture {
		private final AlgorithmLlmReviewConfigMapper configMapper = mock(AlgorithmLlmReviewConfigMapper.class);
		private final LlmReviewTaskMapper taskMapper = mock(LlmReviewTaskMapper.class);
		private final LlmProviderMapper providerMapper = mock(LlmProviderMapper.class);
		private final OpenAiVisionClient visionClient = mock(OpenAiVisionClient.class);
		private final LlmReviewTaskService service;

		Fixture(boolean enabled) {
			VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
			properties.setEnabled(enabled);
			service = new LlmReviewTaskService(properties, configMapper, providerMapper,
				taskMapper, mock(DeviceMediaUploadService.class), visionClient,
				mock(ActiveSafetyEventReportService.class));
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
