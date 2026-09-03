/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.job;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask;
import com.ruoyi.vlstream.test.vlstream.service.LlmReviewTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Claims review tasks so MQTT ingestion never waits for an external model. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmReviewWorker {

	private final VlsLlmReviewProperties properties;
	private final LlmReviewTaskMapper taskMapper;
	private final LlmReviewTaskService taskService;
	private final String workerId = UUID.randomUUID().toString();

	@Scheduled(fixedDelayString = "${vlstream.llm-review.scan-interval-millis:3000}")
	public void processPending() {
		if (!Boolean.TRUE.equals(properties.getEnabled())) {
			return;
		}
		Date now = new Date();
		long stale = properties.getStaleLockMillis() == null ? 120000L : properties.getStaleLockMillis();
		int batchSize = Math.max(1, Math.min(properties.getBatchSize() == null ? 10 : properties.getBatchSize(), 50));
		if (taskMapper.claimBatch(workerId, now, new Date(now.getTime() - stale), batchSize) == 0) {
			return;
		}
		List<LlmReviewTask> tasks = taskMapper.selectClaimed(workerId);
		for (LlmReviewTask task : tasks) {
			String previousTenant = TenantContextHolder.getTenantId();
			TenantContextHolder.setTenantId(task.getTenantId());
			try {
				taskService.processClaimed(task, workerId);
			} catch (Exception exception) {
				log.error("大模型复核任务处理异常: taskId={}", task.getId(), exception);
			} finally {
				TenantContextHolder.setTenantId(previousTenant);
			}
		}
	}
}
