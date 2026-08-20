/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** Commits local delivery state without holding a transaction over HTTP. */
@Service
@RequiredArgsConstructor
public class EventReportDeliveryStateService {

	private final EventReportOutboxMapper outboxMapper;
	private final VlsEventManagementMapper eventManagementMapper;

	@Transactional(rollbackFor = Exception.class)
	public void markSuccess(EventReportOutbox task, String workerId, Date now) {
		if (outboxMapper.markSuccess(task.getId(), task.getTenantId(), workerId, now) != 1) {
			throw new ServiceException("事件上报任务成功状态更新冲突");
		}
		if (eventManagementMapper.markReported(task.getEventId(), task.getTenantId(), now) != 1) {
			throw new ServiceException("事件已上报状态更新失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void markFailure(EventReportOutbox task, String workerId, int retryCount,
						String status, Date nextRetryTime, String error, Date now) {
		String safeError = StringUtils.abbreviate(StringUtils.defaultIfBlank(error, "未知上报异常"), 2000);
		if (outboxMapper.markFailure(task.getId(), task.getTenantId(), workerId, status,
			retryCount, nextRetryTime, safeError, now) != 1) {
			throw new ServiceException("事件上报任务失败状态更新冲突");
		}
	}
}
