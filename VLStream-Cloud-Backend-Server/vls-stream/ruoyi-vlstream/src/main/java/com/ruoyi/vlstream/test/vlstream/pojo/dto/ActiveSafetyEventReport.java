/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Transport-neutral active-safety event passed from an IoT adapter to the task module.
 */
@Data
@Builder
public class ActiveSafetyEventReport {

	private String sourceMessageId;
	private String deviceEventId;
	private String deviceId;
	private String deviceName;
	private String deviceTag;
	private String eventType;
	private String description;
	private Date eventTime;
	private String mediaId;
	private String address;
	private BigDecimal longitude;
	private BigDecimal latitude;
}
