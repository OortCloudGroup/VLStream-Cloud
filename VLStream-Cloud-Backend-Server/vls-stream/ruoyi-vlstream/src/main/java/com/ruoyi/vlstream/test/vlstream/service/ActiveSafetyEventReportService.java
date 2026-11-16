/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;

/**
 * Shared active-safety event sink used by HTTP and MQTT adapters.
 */
public interface ActiveSafetyEventReportService {

	/** Load the stored event and its media references within the trusted tenant. */
	JSONObject loadReviewSource(String eventId, String tenantId);

	ActiveSafetyEventReportResult findDuplicate(String sourceMessageId, String deviceId,
											   String deviceEventId);

	ActiveSafetyEventReportResult report(ActiveSafetyEventReport report);
}
