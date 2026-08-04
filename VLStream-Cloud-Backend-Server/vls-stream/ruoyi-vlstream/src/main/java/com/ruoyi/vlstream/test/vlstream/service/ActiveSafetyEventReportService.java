/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;

/**
 * Shared active-safety event sink used by HTTP and MQTT adapters.
 */
public interface ActiveSafetyEventReportService {

	ActiveSafetyEventReportResult findDuplicate(String sourceMessageId, String deviceId,
											   String deviceEventId);

	ActiveSafetyEventReportResult report(ActiveSafetyEventReport report);
}
