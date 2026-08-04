/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Result of persisting or locating one active-safety event.
 */
@Data
@Builder
public class ActiveSafetyEventReportResult {

	private String activeSafetyEventId;
	private String mediaId;
	private boolean duplicate;
}
