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

import java.util.Date;

/** OTA task fields safe for management-page display. */
@Data
@Builder
public class FirmwareDeployTaskView {
	private String requestId;
	private String target;
	private String currentVersion;
	private String targetVersion;
	private String deployStatus;
	private Date publishedAt;
	private Date lastReplyAt;
	private Date completedAt;
	private String failureReason;
}
