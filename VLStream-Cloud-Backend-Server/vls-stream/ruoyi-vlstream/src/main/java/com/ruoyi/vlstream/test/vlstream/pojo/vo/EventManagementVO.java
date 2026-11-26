/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * event
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EventManagementVO extends EventManagement {
	private static final long serialVersionUID = 1L;

	/** Read-only position with the source of the displayed address or coordinates. */
	private Map<String, Object> displayLocation;

	/** Temporary view URL for a private event image. */
	private String reportImageUrl;

	/** Older event pages render reportImg directly. Keep the stored value out of the view URL generation. */
	@JsonGetter("reportImg")
	public String reportImageForDisplay() {
		return StringUtils.defaultIfBlank(reportImageUrl, getReportImg());
	}

	/** Publish the display position for older pages without changing the persisted event address. */
	@JsonGetter("reportLocation")
	public String reportLocationForDisplay() {
		if (StringUtils.isNotBlank(getReportLocation()) || displayLocation == null) return getReportLocation();
		Object address = displayLocation.get("address");
		Object longitude = displayLocation.get("longitude");
		Object latitude = displayLocation.get("latitude");
		String text = address instanceof String ? ((String) address).trim() : "";
		if (text.isEmpty() && longitude != null && latitude != null) text = longitude + ", " + latitude;
		if (text.isEmpty()) return getReportLocation();
		return "DEVICE_HEARTBEAT".equals(displayLocation.get("source")) ? "设备位置参考：" + text : text;
	}
}
