/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
	/** Validated local references, split by their registered MIME rather than device hints. */
	private java.util.List<String> imageMediaIds;
	private java.util.List<String> videoMediaIds;
	private boolean externalMedia;
	private String mqttPayloadJson;
	private String address;
	private BigDecimal longitude;
	private BigDecimal latitude;
}
