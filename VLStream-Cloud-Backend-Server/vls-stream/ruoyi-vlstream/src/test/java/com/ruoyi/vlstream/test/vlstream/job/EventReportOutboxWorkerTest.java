/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.job;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsEventReportProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import com.ruoyi.vlstream.test.vlstream.service.DeviceMediaUploadService;
import com.ruoyi.vlstream.test.vlstream.service.EventReportDeliveryStateService;
import com.ruoyi.vlstream.test.vlstream.service.ThirdPartyEventReportClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("dev")
class EventReportOutboxWorkerTest {

	@AfterEach
	void clearTenant() {
		TenantContextHolder.clear();
	}

	@Test
	void schedulesRetryAndClearsTenantWhenDeliveryFails() throws Exception {
		EventReportOutboxMapper mapper = mock(EventReportOutboxMapper.class);
		EventReportDeliveryStateService stateService = mock(EventReportDeliveryStateService.class);
		ThirdPartyEventReportClient client = mock(ThirdPartyEventReportClient.class);
		DeviceMediaUploadService mediaService = mock(DeviceMediaUploadService.class);
		VlsEventReportProperties properties = new VlsEventReportProperties();
		properties.setBaseRetryMillis(1000L);
		properties.setMaxRetryMillis(10000L);

		EventReportOutbox task = new EventReportOutbox();
		task.setId(1L);
		task.setEventId(2L);
		task.setTenantId("tenant-a");
		task.setIdempotencyKey("event-2");
		task.setPayloadJson("{\"device_tenant_id\":\"tenant-a\",\"pics\":[]}");
		task.setRetryCount(0);
		when(mapper.claimBatch(anyString(), any(Date.class), any(Date.class), anyInt())).thenReturn(1);
		when(mapper.selectClaimed(anyString())).thenReturn(Collections.singletonList(task));
		doThrow(new IllegalStateException("downstream unavailable"))
			.when(client).report(any(), anyString());

		EventReportOutboxWorker worker = new EventReportOutboxWorker(
			mapper, stateService, client, mediaService, properties);
		setField(worker, "tenantType", "multi");
		worker.deliverPendingEvents();

		ArgumentCaptor<Integer> retryCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
		verify(stateService).markFailure(any(), anyString(), retryCaptor.capture(),
			statusCaptor.capture(), any(Date.class), anyString(), any(Date.class));
		assertEquals(1, retryCaptor.getValue());
		assertEquals("RETRY", statusCaptor.getValue());
		assertNull(TenantContextHolder.getTenantId());
	}

	@Test
	void resolvesImagesAndVideoInTaskTenantAndRestoresPreviousContext() throws Exception {
		EventReportOutboxMapper mapper = mock(EventReportOutboxMapper.class);
		EventReportDeliveryStateService state = mock(EventReportDeliveryStateService.class);
		ThirdPartyEventReportClient client = mock(ThirdPartyEventReportClient.class);
		DeviceMediaUploadService media = mock(DeviceMediaUploadService.class);
		EventReportOutbox task = task("tenant-a");
		when(mapper.claimBatch(anyString(), any(), any(), anyInt())).thenReturn(1);
		when(mapper.selectClaimed(anyString())).thenReturn(Collections.singletonList(task));
		when(media.getPrivateViewUrl(anyString(), anyInt())).thenAnswer(call -> {
			assertEquals("tenant-a", TenantContextHolder.getTenantId());
			return "https://media.invalid/" + call.getArgument(0);
		});
		EventReportOutboxWorker worker = new EventReportOutboxWorker(mapper, state, client, media, new VlsEventReportProperties());
		setField(worker, "tenantType", "multi");
		TenantContextHolder.setTenantId("previous");
		worker.deliverPendingEvents();
		ArgumentCaptor<cn.hutool.json.JSONObject> payload = ArgumentCaptor.forClass(cn.hutool.json.JSONObject.class);
		verify(client).report(payload.capture(), anyString());
		assertEquals("https://media.invalid/image-1", payload.getValue().getJSONArray("pics").getStr(0));
		assertEquals("https://media.invalid/video-1", payload.getValue().getJSONArray("video").getStr(0));
		verify(state).markSuccess(any(), anyString(), any());
		assertEquals("previous", TenantContextHolder.getTenantId());
	}

	@Test
	void rejectsPayloadTenantMismatchBeforeAnyExternalCall() throws Exception {
		EventReportOutboxMapper mapper = mock(EventReportOutboxMapper.class);
		EventReportDeliveryStateService state = mock(EventReportDeliveryStateService.class);
		ThirdPartyEventReportClient client = mock(ThirdPartyEventReportClient.class);
		DeviceMediaUploadService media = mock(DeviceMediaUploadService.class);
		EventReportOutbox task = task("tenant-b");
		when(mapper.claimBatch(anyString(), any(), any(), anyInt())).thenReturn(1);
		when(mapper.selectClaimed(anyString())).thenReturn(Collections.singletonList(task));
		EventReportOutboxWorker worker = new EventReportOutboxWorker(mapper, state, client, media, new VlsEventReportProperties());
		setField(worker, "tenantType", "multi");
		worker.deliverPendingEvents();
		verifyNoInteractions(client, media);
		verify(state, never()).markSuccess(any(), anyString(), any());
		verify(state).markFailure(any(), anyString(), anyInt(), anyString(), any(), anyString(), any());
		assertNull(TenantContextHolder.getTenantId());
	}

	private EventReportOutbox task(String tenantId) {
		EventReportOutbox task = new EventReportOutbox();
		task.setId(1L); task.setEventId(2L); task.setTenantId(tenantId); task.setRetryCount(0);
		task.setIdempotencyKey("event-2");
		task.setPayloadJson("{\"device_tenant_id\":\"tenant-a\",\"pics\":[\"vls-media://image-1\"],\"video\":[\"vls-media://video-1\"]}");
		return task;
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
