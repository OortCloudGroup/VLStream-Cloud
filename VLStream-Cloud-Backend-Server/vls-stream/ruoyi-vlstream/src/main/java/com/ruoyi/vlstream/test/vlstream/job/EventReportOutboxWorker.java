/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.job;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsEventReportProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import com.ruoyi.vlstream.test.vlstream.service.DeviceMediaUploadService;
import com.ruoyi.vlstream.test.vlstream.service.EventReportDeliveryStateService;
import com.ruoyi.vlstream.test.vlstream.service.ThirdPartyEventReportClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Claims and delivers durable event reports without an HTTP authentication token. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventReportOutboxWorker {

	private static final String MEDIA_REFERENCE_PREFIX = "vls-media://";
	private static final String MULTI = "multi";

	private final EventReportOutboxMapper outboxMapper;
	private final EventReportDeliveryStateService stateService;
	private final ThirdPartyEventReportClient reportClient;
	private final DeviceMediaUploadService mediaUploadService;
	private final VlsEventReportProperties properties;
	private final String workerId = UUID.randomUUID().toString();

	@Value("${token.tenant-type:single}")
	private String tenantType;

	@Scheduled(fixedDelayString = "${vlstream.event-report.scan-interval-millis:5000}")
	public void deliverPendingEvents() {
		if (!Boolean.TRUE.equals(properties.getEnabled()) || !MULTI.equalsIgnoreCase(tenantType)) {
			return;
		}
		Date now = new Date();
		long staleMillis = positive(properties.getStaleLockMillis(), 60000L);
		int batchSize = Math.max(1, Math.min(properties.getBatchSize() == null ? 50 : properties.getBatchSize(), 200));
		int claimed = outboxMapper.claimBatch(workerId, now, new Date(now.getTime() - staleMillis), batchSize);
		if (claimed == 0) {
			return;
		}
		List<EventReportOutbox> tasks = outboxMapper.selectClaimed(workerId);
		for (EventReportOutbox task : tasks) {
			deliverOne(task);
		}
	}

	private void deliverOne(EventReportOutbox task) {
		String previousTenant = TenantContextHolder.getTenantId();
		TenantContextHolder.setTenantId(task.getTenantId());
		try {
			JSONObject payload = JSONUtil.parseObj(task.getPayloadJson());
			resolveMediaReferences(payload);
			reportClient.report(payload, task.getIdempotencyKey());
			stateService.markSuccess(task, workerId, new Date());
			log.info("第三方事件上报成功: eventId={}, tenantId={}", task.getEventId(), task.getTenantId());
		} catch (Exception exception) {
			handleFailure(task, exception);
		} finally {
			TenantContextHolder.setTenantId(previousTenant);
		}
	}

	private void resolveMediaReferences(JSONObject payload) {
		JSONArray pics = payload.getJSONArray("pics");
		if (pics == null) {
			return;
		}
		for (int index = 0; index < pics.size(); index++) {
			String value = pics.getStr(index);
			if (StringUtils.startsWith(value, MEDIA_REFERENCE_PREFIX)) {
				String mediaId = value.substring(MEDIA_REFERENCE_PREFIX.length());
				int ttl = Math.max(60, Math.min(properties.getMediaUrlTtlSeconds() == null
					? 3600 : properties.getMediaUrlTtlSeconds(), 86400));
				pics.set(index, mediaUploadService.getPrivateViewUrl(mediaId, ttl));
			}
		}
	}

	private void handleFailure(EventReportOutbox task, Exception exception) {
		int retryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
		int maxRetries = Math.max(1, properties.getMaxRetries() == null ? 10 : properties.getMaxRetries());
		String status = retryCount >= maxRetries ? "DEAD" : "RETRY";
		long delay = retryDelay(retryCount);
		Date now = new Date();
		Date nextRetry = new Date(now.getTime() + delay);
		try {
			stateService.markFailure(task, workerId, retryCount, status, nextRetry,
				exception.getMessage(), now);
		} catch (Exception stateException) {
			log.error("事件上报失败状态无法保存: outboxId={}", task.getId(), stateException);
		}
		log.warn("第三方事件上报失败: eventId={}, tenantId={}, retryCount={}, status={}, reason={}",
			task.getEventId(), task.getTenantId(), retryCount, status, exception.getMessage());
	}

	private long retryDelay(int retryCount) {
		long base = positive(properties.getBaseRetryMillis(), 10000L);
		long maximum = positive(properties.getMaxRetryMillis(), 1800000L);
		int exponent = Math.max(0, Math.min(retryCount - 1, 20));
		long multiplier = 1L << exponent;
		return Math.min(maximum, base > Long.MAX_VALUE / multiplier ? maximum : base * multiplier);
	}

	private long positive(Long value, long fallback) {
		return value == null || value <= 0 ? fallback : value;
	}
}
