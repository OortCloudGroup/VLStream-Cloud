/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** One durable third-party event delivery attempt stream. */
@Data
@TableName("vls_event_report_outbox")
public class EventReportOutbox {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private String tenantId;
	private Long eventId;
	private String idempotencyKey;
	private String payloadJson;
	private String status;
	private Integer retryCount;
	private Date nextRetryTime;
	private String lockedBy;
	private Date lockedAt;
	private String lastError;
	private Date reportedAt;
	private Date createTime;
	private Date updateTime;
}
